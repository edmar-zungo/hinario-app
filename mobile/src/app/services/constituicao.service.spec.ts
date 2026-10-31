import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ConstituicaoService } from './constituicao.service';
import { Constituicao } from '../model/constituicao.model';
import { environment } from 'src/environments/environment';

const FIXTURE: Constituicao = {
  id: 'constituicao-iera',
  titulo: 'Constituição da IERA',
  fonte: 'fonte.pdf',
  paginasFonte: 69,
  esquemaVersao: '1.0',
  fonteEsquemaVersao: '2.0',
  geradoDe: 'constituicao_iera_v2.json',
  geradoEm: '2026-10-31T00:00:00.000Z',
  totalArtigos: 2,
  indice: [],
  partes: [
    {
      id: 'I',
      numero: 'I',
      titulo: 'ESTATUTOS',
      paginaInicio: 4,
      textosFrontal: [
        {
          id: 'I-preambulo',
          titulo: 'PREÂMBULO',
          paginaInicio: 4,
          paragrafos: ['Texto do prémbulo sobre a Missão do Norte de Angola.'],
        },
      ],
      capitulos: [
        {
          id: 'I-cap-1',
          numero: 'I',
          titulo: 'NOME, SEDE E ÂMBITO',
          paginaInicio: 5,
          artigos: [
            {
              id: 'I-art-1',
              numero: 1,
              rotulo: 'Artigo 1º',
              titulo: 'DENOMINAÇÃO',
              paginaInicio: 5,
              paginaFim: 5,
              conteudo: [
                {
                  marcador: null,
                  tipo: 'texto',
                  texto:
                    'A presente comunidade cristã denomina-se Igreja Evangélica Reformada de Angola.',
                },
              ],
            },
            {
              id: 'I-art-2',
              numero: 2,
              rotulo: 'Artigo 2º',
              titulo: 'SEDE E ÂMBITO',
              paginaInicio: 5,
              paginaFim: 5,
              conteudo: [
                {
                  marcador: '1',
                  tipo: 'numero',
                  texto: 'A sede administrativa da IERA é em Luanda.',
                },
              ],
            },
          ],
          seccoes: [],
        },
      ],
    },
  ],
  apendices: [
    {
      id: 'apendice-emendas',
      titulo: 'EMENDAS',
      paginaInicio: 69,
      paragrafos: ['Primeira Emenda - foi introduzida em 1979.'],
    },
  ],
};

describe('ConstituicaoService', () => {
  let service: ConstituicaoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ConstituicaoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function carregarFixture() {
    service.getConstituicao().subscribe();
    const req = httpMock.expectOne(environment.constituicaoUrl);
    req.flush(FIXTURE);
  }

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('removerAcentos normaliza para pesquisa insensível a acentos', () => {
    expect(ConstituicaoService.removerAcentos('SÉDE E ÂMBITO')).toBe(
      'sede e ambito'
    );
  });

  it('monta as unidades de leitura por ordem documental', () => {
    carregarFixture();
    service.getUnidades().subscribe((unidades) => {
      expect(unidades.map((u) => u.id)).toEqual([
        'I-preambulo',
        'I-art-1',
        'I-art-2',
        'apendice-emendas',
      ]);
    });
  });

  it('encontra artigo por id com adjacentes', () => {
    carregarFixture();
    service.getUnidadeById('I-art-1').subscribe((unidade) => {
      expect(unidade?.titulo).toBe('DENOMINAÇÃO');
      expect(unidade?.artigo?.id).toBe('I-art-1');
    });
    service.getAdjacentes('I-art-1').subscribe((adj) => {
      expect(adj.anterior?.id).toBe('I-preambulo');
      expect(adj.seguinte?.id).toBe('I-art-2');
    });
  });

  it('pesquisa sem acentos devolve trecho do texto', () => {
    carregarFixture();
    service.pesquisar('luanda').subscribe((resultados) => {
      expect(resultados.length).toBe(1);
      expect(resultados[0].unidade.id).toBe('I-art-2');
      expect(resultados[0].trecho).toContain('Luanda');
    });
    service.pesquisar('MISSAO').subscribe((resultados) => {
      expect(resultados.length).toBe(1);
      expect(resultados[0].unidade.id).toBe('I-preambulo');
    });
  });

  it('pesquisa vazia devolve lista vazia', () => {
    service.pesquisar('   ').subscribe((resultados) => {
      expect(resultados).toEqual([]);
    });
    httpMock.expectNone(environment.constituicaoUrl);
  });
});
