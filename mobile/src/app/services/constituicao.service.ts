import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable, of, shareReplay } from 'rxjs';
import { environment } from 'src/environments/environment';
import {
  BlocoConteudo,
  Constituicao,
  ParteConstituicao,
  ResultadoPesquisaConstituicao,
  UnidadeLeitura,
} from '../model/constituicao.model';

@Injectable({
  providedIn: 'root',
})
export class ConstituicaoService {
  private cache$: Observable<Constituicao | null> | null = null;
  private unidadesEmMemoria: UnidadeLeitura[] | null = null;

  constructor(private http: HttpClient) {}

  public static removerAcentos(texto: string): string {
    if (!texto) {
      return '';
    }
    return texto
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }

  private static semTrim(texto: string): string {
    if (!texto) {
      return '';
    }
    return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  }

  getConstituicao(): Observable<Constituicao | null> {
    if (!this.cache$) {
      this.cache$ = this.http.get<Constituicao>(environment.constituicaoUrl).pipe(
        map((c) => this.garantirEstrutura(c)),
        shareReplay(1)
      );
    }
    return this.cache$;
  }

  getPartes(): Observable<ParteConstituicao[]> {
    return this.getConstituicao().pipe(
      map((c) => (c ? c.partes : []))
    );
  }

  getUnidades(): Observable<UnidadeLeitura[]> {
    if (this.unidadesEmMemoria) {
      return of(this.unidadesEmMemoria);
    }
    return this.getConstituicao().pipe(
      map((c) => {
        const unidades = c ? this.montarUnidades(c) : [];
        this.unidadesEmMemoria = unidades;
        return unidades;
      })
    );
  }

  getUnidadeById(id: string): Observable<UnidadeLeitura | undefined> {
    return this.getUnidades().pipe(
      map((unidades) => unidades.find((u) => u.id === id))
    );
  }

  getAdjacentes(
    unidadeId: string
  ): Observable<{ anterior?: UnidadeLeitura; seguinte?: UnidadeLeitura }> {
    return this.getUnidades().pipe(
      map((unidades) => {
        const index = unidades.findIndex((u) => u.id === unidadeId);
        if (index === -1) {
          return { anterior: undefined, seguinte: undefined };
        }
        return {
          anterior: index > 0 ? unidades[index - 1] : undefined,
          seguinte:
            index < unidades.length - 1 ? unidades[index + 1] : undefined,
        };
      })
    );
  }

  pesquisar(termo: string): Observable<ResultadoPesquisaConstituicao[]> {
    const termoNorm = ConstituicaoService.removerAcentos(termo);
    if (!termoNorm) {
      return of([]);
    }

    return this.getUnidades().pipe(
      map((unidades) => {
        const resultados: ResultadoPesquisaConstituicao[] = [];
        for (const unidade of unidades) {
          if (
            ConstituicaoService.removerAcentos(unidade.titulo).includes(
              termoNorm
            ) ||
            ConstituicaoService.removerAcentos(unidade.rotulo).includes(
              termoNorm
            )
          ) {
            resultados.push({
              unidade,
              trecho: this.primeiroTrecho(unidade, termoNorm) ?? this.abrirTrecho(unidade),
            });
            continue;
          }
          const trecho = this.primeiroTrecho(unidade, termoNorm);
          if (trecho !== null) {
            resultados.push({ unidade, trecho });
          }
        }
        return resultados;
      })
    );
  }

  private garantirEstrutura(c: Constituicao): Constituicao {
    return {
      ...c,
      partes: (c.partes || []).map((p) => ({
        ...p,
        textosFrontal: p.textosFrontal || [],
        capitulos: (p.capitulos || []).map((cap) => ({
          ...cap,
          artigos: cap.artigos || [],
          seccoes: (cap.seccoes || []).map((s) => ({
            ...s,
            artigos: s.artigos || [],
          })),
        })),
      })),
      apendices: c.apendices || [],
      indice: c.indice || [],
    };
  }

  private montarUnidades(constituicao: Constituicao): UnidadeLeitura[] {
    const unidades: UnidadeLeitura[] = [];

    for (const parte of constituicao.partes) {
      const contextoParte = `Parte ${parte.numero}`;

      for (const frontal of parte.textosFrontal) {
        unidades.push({
          id: frontal.id,
          tipo: 'texto',
          rotulo: frontal.titulo,
          titulo: frontal.titulo,
          contexto: contextoParte,
          paragrafos: frontal.paragrafos,
        });
      }

      for (const capitulo of parte.capitulos) {
        const contextoCap =
          `${contextoParte} · Cap. ${capitulo.numero} — ${capitulo.titulo}`;
        for (const artigo of capitulo.artigos) {
          unidades.push({
            id: artigo.id,
            tipo: 'artigo',
            rotulo: artigo.rotulo,
            titulo: artigo.titulo,
            contexto: contextoCap,
            artigo,
          });
        }
        for (const seccao of capitulo.seccoes) {
          const contextoSec =
            `${contextoParte} · Cap. ${capitulo.numero} · ` +
            `Secção ${seccao.numero} — ${seccao.titulo}`;
          for (const artigo of seccao.artigos) {
            unidades.push({
              id: artigo.id,
              tipo: 'artigo',
              rotulo: artigo.rotulo,
              titulo: artigo.titulo,
              contexto: contextoSec,
              artigo,
            });
          }
        }
      }
    }

    for (const apendice of constituicao.apendices) {
      unidades.push({
        id: apendice.id,
        tipo: 'texto',
        rotulo: apendice.titulo,
        titulo: apendice.titulo,
        contexto: 'Apêndice',
        paragrafos: apendice.paragrafos,
      });
    }

    return unidades;
  }

  private textosDaUnidade(unidade: UnidadeLeitura): string[] {
    if (unidade.tipo === 'texto') {
      return unidade.paragrafos || [];
    }
    const textos: string[] = [];
    const visitar = (bloco: BlocoConteudo) => {
      textos.push(bloco.texto);
      for (const item of bloco.itens || []) {
        visitar(item);
      }
    };
    for (const bloco of unidade.artigo?.conteudo || []) {
      visitar(bloco);
    }
    return textos;
  }

  private primeiroTrecho(
    unidade: UnidadeLeitura,
    termoNorm: string
  ): string | null {
    for (const texto of this.textosDaUnidade(unidade)) {
      const trecho = this.montarTrecho(texto, termoNorm);
      if (trecho !== null) {
        return trecho;
      }
    }
    return null;
  }

  private abrirTrecho(unidade: UnidadeLeitura): string {
    const texto = this.textosDaUnidade(unidade)[0] || unidade.titulo;
    return texto.length > 140 ? texto.slice(0, 140).trim() + '…' : texto;
  }

  private montarTrecho(texto: string, termoNorm: string): string | null {
    const indice = this.indiceOriginal(texto, termoNorm);
    if (indice < 0) {
      return null;
    }
    const inicio = Math.max(0, indice - 60);
    const fim = Math.min(texto.length, indice + termoNorm.length + 90);
    return (
      (inicio > 0 ? '…' : '') +
      texto.slice(inicio, fim).trim() +
      (fim < texto.length ? '…' : '')
    );
  }

  private indiceOriginal(texto: string, termoNorm: string): number {
    const normalizado = ConstituicaoService.semTrim(texto);
    const idxNorm = normalizado.indexOf(termoNorm);
    if (idxNorm < 0) {
      return -1;
    }
    let acumulado = 0;
    for (let i = 0; i < texto.length; i++) {
      const tamanho = ConstituicaoService.semTrim(texto[i]).length;
      if (acumulado + tamanho > idxNorm) {
        return i;
      }
      acumulado += tamanho;
    }
    return -1;
  }
}
