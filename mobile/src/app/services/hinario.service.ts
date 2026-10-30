import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, forkJoin, map, Observable, of, shareReplay, tap } from 'rxjs';
import { Preferences } from '@capacitor/preferences';
import { environment } from 'src/environments/environment';
import { HinarioExport, HinoExport } from '../model/hino-export.model';
import { HinoModel } from '../model/hino-model';
import { EstrofeModel } from '../model/estrofe-model';
import { Linguas, LINGUA_MAP } from '../enums/linguas';

const PREF_FAVORITOS_KEY = 'favoritos_ids';
const PREF_FONT_SIZE_KEY = 'leitor_font_size';
const PREF_TEMA_KEY = 'leitor_tema';

export type TemaLeitor = 'claro' | 'sepia' | 'escuro';

@Injectable({
  providedIn: 'root',
})
export class HinarioService {
  private hinariosCache$: Observable<HinarioExport[]> | null = null;
  private hinariosEmMemoria: HinarioExport[] | null = null;
  private todosHinosEmMemoria: HinoExport[] | null = null;

  private favoritosSubject = new BehaviorSubject<string[]>([]);
  public favoritos$ = this.favoritosSubject.asObservable();

  constructor(private http: HttpClient) {
    this.inicializarFavoritos();
  }

  /**
   * Remove acentos e normaliza texto para comparação/pesquisa insensível a acentos
   */
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

  /**
   * Carregamento único em memória de todos os 4 hinários exportados
   */
  getHinarios(): Observable<HinarioExport[]> {
    if (this.hinariosEmMemoria) {
      return of(this.hinariosEmMemoria);
    }

    if (!this.hinariosCache$) {
      const urls = [
        { url: environment.hinarioPorUrl || './assets/data/hinario-por.json', id: 'por', nome: 'Hinário Português' },
        { url: environment.hinarioKkoUrl || './assets/data/hinario-kko.json', id: 'kko', nome: 'Hinário Kikongo (origem)' },
        { url: environment.hinarioKkrUrl || './assets/data/hinario-kkr.json', id: 'kkr', nome: 'Hinário Kikongo (revisado)' },
        { url: environment.hinarioKmbUrl || './assets/data/hinario-kmb.json', id: 'kmb', nome: 'Hinário Kimbundu' },
      ];

      this.hinariosCache$ = forkJoin(
        urls.map((u) =>
          this.http.get<HinarioExport>(u.url).pipe(
            map((hinario) => {
              const hinarioCompleto: HinarioExport = {
                ...hinario,
                id: hinario.id || u.id,
                lingua: hinario.lingua || u.id,
                nome: hinario.nome || u.nome,
                hinos: (hinario.hinos || []).map((hino) => ({
                  ...hino,
                  lingua: hinario.id || u.id,
                  hinarioNome: hinario.nome || u.nome,
                })),
              };
              return hinarioCompleto;
            })
          )
        )
      ).pipe(
        tap((hinarios) => {
          this.hinariosEmMemoria = hinarios;
          this.todosHinosEmMemoria = hinarios.flatMap((h) => h.hinos);
        }),
        shareReplay(1)
      );
    }

    return this.hinariosCache$;
  }

  /**
   * Retorna todos os hinos de todos os hinários (carregados em memória)
   */
  getAllHinosExport(): Observable<HinoExport[]> {
    if (this.todosHinosEmMemoria) {
      return of(this.todosHinosEmMemoria);
    }
    return this.getHinarios().pipe(
      map((hinarios) => hinarios.flatMap((h) => h.hinos))
    );
  }

  /**
   * Retorna os hinos de uma língua específica (ex: 'por', 'kko', 'kkr', 'kmb')
   */
  getHinosByLingua(linguaId: string): Observable<HinoExport[]> {
    const cod = LINGUA_MAP[linguaId] || linguaId;
    return this.getHinarios().pipe(
      map((hinarios) => {
        const found = hinarios.find((h) => h.id === cod || h.lingua === cod);
        return found ? found.hinos : [];
      })
    );
  }

  /**
   * Procura um hino por ID
   */
  getHinoById(id: string): Observable<HinoExport | undefined> {
    return this.getAllHinosExport().pipe(
      map((hinos) => hinos.find((h) => h.id === id))
    );
  }

  /**
   * Procura hino anterior e seguinte no MESMO hinário/língua
   */
  getAdjacentHinos(
    hinoId: string
  ): Observable<{ anterior?: HinoExport; seguinte?: HinoExport }> {
    return this.getHinarios().pipe(
      map((hinarios) => {
        for (const hinario of hinarios) {
          const index = hinario.hinos.findIndex((h) => h.id === hinoId);
          if (index !== -1) {
            return {
              anterior: index > 0 ? hinario.hinos[index - 1] : undefined,
              seguinte:
                index < hinario.hinos.length - 1
                  ? hinario.hinos[index + 1]
                  : undefined,
            };
          }
        }
        return { anterior: undefined, seguinte: undefined };
      })
    );
  }

  /**
   * Pesquisa sem acentos no número, página, título e texto das estrofes/coros
   */
  pesquisar(termo: string, lingua?: string): Observable<HinoExport[]> {
    const termoNormalizado = HinarioService.removerAcentos(termo);
    if (!termoNormalizado) {
      return of([]);
    }

    const fonte$ = lingua && lingua !== 'todos'
      ? this.getHinosByLingua(lingua)
      : this.getAllHinosExport();

    return fonte$.pipe(
      map((hinos) =>
        hinos.filter((hino) => {
          // Número exacto ou contido
          if (hino.numero.toString().includes(termoNormalizado)) {
            return true;
          }
          // Página
          if (hino.pagina && hino.pagina.toString().includes(termoNormalizado)) {
            return true;
          }
          // Título sem acentos
          const tituloNorm = HinarioService.removerAcentos(hino.titulo);
          if (tituloNorm.includes(termoNormalizado)) {
            return true;
          }
          // Autor
          if (hino.autor) {
            const autorNorm = HinarioService.removerAcentos(hino.autor);
            if (autorNorm.includes(termoNormalizado)) {
              return true;
            }
          }
          // Texto das estrofes / coro
          if (hino.blocos) {
            for (const bloco of hino.blocos) {
              if (bloco.linhas) {
                for (const linha of bloco.linhas) {
                  if (
                    HinarioService.removerAcentos(linha.texto).includes(
                      termoNormalizado
                    )
                  ) {
                    return true;
                  }
                }
              }
            }
          }
          return false;
        })
      )
    );
  }

  /**
   * "Ir ao nº": Procura directa por número de hino
   */
  buscarPorNumero(numero: number, lingua?: string): Observable<HinoExport[]> {
    const fonte$ = lingua && lingua !== 'todos'
      ? this.getHinosByLingua(lingua)
      : this.getAllHinosExport();

    return fonte$.pipe(
      map((hinos) => hinos.filter((h) => h.numero === numero))
    );
  }

  /* ==========================================================================
     GESTÃO DE FAVORITOS (IDs em @capacitor/preferences + migração legacy)
     ========================================================================== */

  private async inicializarFavoritos() {
    try {
      const { value } = await Preferences.get({ key: PREF_FAVORITOS_KEY });
      let ids: string[] = [];
      if (value) {
        try {
          ids = JSON.parse(value);
        } catch {
          ids = [];
        }
      }

      // Migração de favoritos antigos do localStorage ('hinos')
      const legacy = localStorage.getItem('hinos');
      if (legacy) {
        try {
          const legacyItems: any[] = JSON.parse(legacy);
          if (Array.isArray(legacyItems)) {
            let migrou = false;
            for (const item of legacyItems) {
              const id = item?.id;
              if (id && !ids.includes(id)) {
                ids.push(id);
                migrou = true;
              }
            }
            if (migrou) {
              await Preferences.set({
                key: PREF_FAVORITOS_KEY,
                value: JSON.stringify(ids),
              });
            }
          }
        } catch (err) {
          console.warn('Erro ao migrar favoritos antigos:', err);
        }
      }

      this.favoritosSubject.next(ids);
    } catch (err) {
      console.warn('Erro ao inicializar preferências de favoritos:', err);
      // Fallback para localStorage
      const fallback = localStorage.getItem(PREF_FAVORITOS_KEY);
      this.favoritosSubject.next(fallback ? JSON.parse(fallback) : []);
    }
  }

  async getFavoritosIds(): Promise<string[]> {
    try {
      const { value } = await Preferences.get({ key: PREF_FAVORITOS_KEY });
      return value ? JSON.parse(value) : [];
    } catch {
      return this.favoritosSubject.getValue();
    }
  }

  async isFavorito(hinoId: string): Promise<boolean> {
    const ids = this.favoritosSubject.getValue();
    return ids.includes(hinoId);
  }

  async toggleFavorito(hinoId: string): Promise<boolean> {
    let ids = await this.getFavoritosIds();
    let isFav = false;

    if (ids.includes(hinoId)) {
      ids = ids.filter((id) => id !== hinoId);
      isFav = false;
    } else {
      ids.push(hinoId);
      isFav = true;
    }

    try {
      await Preferences.set({
        key: PREF_FAVORITOS_KEY,
        value: JSON.stringify(ids),
      });
    } catch {
      localStorage.setItem(PREF_FAVORITOS_KEY, JSON.stringify(ids));
    }

    this.favoritosSubject.next(ids);
    return isFav;
  }

  getHinosFavoritos(): Observable<HinoExport[]> {
    return this.getAllHinosExport().pipe(
      map((todos) => {
        const ids = this.favoritosSubject.getValue();
        return todos.filter((h) => ids.includes(h.id));
      })
    );
  }

  /* ==========================================================================
     TAMANHO DA FONTE E TEMA GUARDADOS (@capacitor/preferences)
     ========================================================================== */

  async getLeitorFontSize(): Promise<number> {
    try {
      const { value } = await Preferences.get({ key: PREF_FONT_SIZE_KEY });
      return value ? parseFloat(value) : 19;
    } catch {
      return 19;
    }
  }

  async setLeitorFontSize(size: number): Promise<void> {
    try {
      await Preferences.set({
        key: PREF_FONT_SIZE_KEY,
        value: size.toString(),
      });
    } catch (e) {
      console.warn('Erro ao guardar tamanho de fonte:', e);
    }
  }

  async getLeitorTema(): Promise<TemaLeitor> {
    try {
      const { value } = await Preferences.get({ key: PREF_TEMA_KEY });
      return (value as TemaLeitor) || 'claro';
    } catch {
      return 'claro';
    }
  }

  async setLeitorTema(tema: TemaLeitor): Promise<void> {
    try {
      await Preferences.set({
        key: PREF_TEMA_KEY,
        value: tema,
      });
    } catch (e) {
      console.warn('Erro ao guardar tema do leitor:', e);
    }
  }

  /* ==========================================================================
     MÉTODOS DE COMPATIBILIDADE COM A INTERFACE LEGADA
     ========================================================================== */

  getAllHinoByJSON(): Observable<HinoModel[]> {
    return this.getAllHinosExport().pipe(
      map((hinosExport) => {
        const favIds = this.favoritosSubject.getValue();
        return hinosExport.map((h) => {
          let mappedLingua = Linguas.PORTUGUES;
          if (h.lingua === 'kmb') mappedLingua = Linguas.KINBUMDU;
          else if (h.lingua === 'kkr') mappedLingua = Linguas.KIKONGO_REVISADO;
          else if (h.lingua === 'kko') mappedLingua = Linguas.KIKONGO_ORIGEM;

          return {
            id: h.id,
            titulo: h.titulo,
            pagina: h.pagina,
            numero: h.numero,
            autor: h.autor || 'IERA',
            dataCriacao: new Date(),
            dataActualizacao: new Date(),
            comentario: '',
            isFavorito: favIds.includes(h.id),
            lingua: mappedLingua,
          };
        });
      })
    );
  }

  getAllEstrofesByJSON(): Observable<EstrofeModel[]> {
    return this.getAllHinosExport().pipe(
      map((hinosExport) => {
        const estrofes: EstrofeModel[] = [];
        for (const hino of hinosExport) {
          if (hino.blocos) {
            hino.blocos.forEach((bloco, idx) => {
              const textoCompleto = (bloco.linhas || [])
                .map((l) => l.texto)
                .join('\n');
              estrofes.push({
                id: `${hino.id}-${idx}`,
                numero: bloco.numero || idx + 1,
                descricao: textoCompleto,
                refrao: bloco.tipo === 'coro',
                hino: { id: hino.id, titulo: hino.titulo },
              });
            });
          }
        }
        return estrofes;
      })
    );
  }

  getAllHinosFavoritos(): HinoModel[] {
    const ids = this.favoritosSubject.getValue();
    if (!this.todosHinosEmMemoria) {
      return [];
    }
    return this.todosHinosEmMemoria
      .filter((h) => ids.includes(h.id))
      .map((h) => ({
        id: h.id,
        titulo: h.titulo,
        pagina: h.pagina,
        numero: h.numero,
        autor: h.autor || 'IERA',
        dataCriacao: new Date(),
        dataActualizacao: new Date(),
        comentario: '',
        isFavorito: true,
        lingua: (h.lingua as any) || Linguas.PORTUGUES,
      }));
  }

  addOuRemoveDosFavoritos(hinoId: string | undefined) {
    if (!hinoId) return;
    this.toggleFavorito(hinoId);
  }

  getStorage(): HinoModel[] | [] {
    return this.getAllHinosFavoritos();
  }

  setStorage(hinos: HinoModel[] | []) {
    const ids = (hinos || []).map((h) => h.id).filter(Boolean) as string[];
    Preferences.set({ key: PREF_FAVORITOS_KEY, value: JSON.stringify(ids) });
    this.favoritosSubject.next(ids);
  }
}
