package br.edu.ifpb.vigisaude.service;

import br.edu.ifpb.vigisaude.model.Estabelecimento;
import br.edu.ifpb.vigisaude.model.Medicamento;
import br.edu.ifpb.vigisaude.repository.EstabelecimentoRepository;
import br.edu.ifpb.vigisaude.repository.MedicamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ETLServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void carregaMapaDeMunicipiosComCodigoDeSeteESeisDigitos() throws Exception {
        Path arquivo = tempDir.resolve("municipios.csv");
        Files.writeString(arquivo, "codigo_ibge,nome,latitude,longitude,capital,codigo_uf,siafi_id,ddd,fuso_horario\n"
                + "2602902,Cabo de Santo Agostinho,-8.28,-35.02,0,26,2357,81,America/Sao_Paulo\n");

        ETLService service = new ETLService(null, null, null, null, null, null, null, null);
        Map<String, String> mapa = service.carregarMapaMunicipios(arquivo.toString());

        assertEquals("Cabo de Santo Agostinho", mapa.get("2602902"));
        assertEquals("Cabo de Santo Agostinho", mapa.get("260290"));
    }

    @Test
    void carregaCnesRemovendoAspasEResolvendoMunicipioUfESemNumero() throws Exception {
        Path municipios = tempDir.resolve("municipios.csv");
        Files.writeString(municipios,
                "codigo_ibge,nome,latitude,longitude,capital,codigo_uf,siafi_id,ddd,fuso_horario\n"
                        + "2602902,Cabo de Santo Agostinho,-8.28,-35.02,0,26,2357,81,America/Sao_Paulo\n");

        Path cnes = tempDir.resolve("cnes.csv");
        String cabecalho = "CO_CNES;CO_UNIDADE;CO_UF;CO_IBGE;NU_CNPJ_MANTENEDORA;NO_RAZAO_SOCIAL;"
                + "NO_FANTASIA;CO_NATUREZA_ORGANIZACAO;DS_NATUREZA_ORGANIZACAO;TP_GESTAO;"
                + "CO_NIVEL_HIERARQUIA;DS_NIVEL_HIERARQUIA;CO_ESFERA_ADMINISTRATIVA;"
                + "DS_ESFERA_ADMINISTRATIVA;CO_ATIVIDADE;TP_UNIDADE;CO_CEP;NO_LOGRADOURO;"
                + "NU_ENDERECO;NO_BAIRRO\n";
        String linha = "\"19\";\"2602900000019\";\"26\";\"260290\";\"11294402000162\";"
                + "\"PREFEITURA MUNICIPAL DO CABO DE SANTO AGOSTINHO\";"
                + "\"POLICLINICA DR JAMACI DE MEDEIROS\";;;\"M\";;;\"M \";\"MUNICIPAL\";"
                + "\"04\";\"4\";\"54580530\";\"RUA 21 DE ABRIL\";\"S/N\";\"PONTE DOS CARVALHOS\"\n";
        Files.writeString(cnes, cabecalho + linha, StandardCharsets.ISO_8859_1);

        EstabelecimentoRepository repository = mock(EstabelecimentoRepository.class);
        when(repository.count()).thenReturn(0L);

        ETLService service = new ETLService(null, repository, null, null, null, null, null, null);
        service.carregarCNES(cnes.toString(), municipios.toString());

        ArgumentCaptor<Estabelecimento> captor = ArgumentCaptor.forClass(Estabelecimento.class);
        verify(repository).save(captor.capture());
        Estabelecimento estabelecimento = captor.getValue();

        assertEquals("0000019", estabelecimento.getCnes());
        assertEquals("PREFEITURA MUNICIPAL DO CABO DE SANTO AGOSTINHO", estabelecimento.getRazaoSocial());
        assertEquals("POLICLINICA DR JAMACI DE MEDEIROS", estabelecimento.getNomeFantasia());
        assertEquals("4", estabelecimento.getTipoUnidade());
        assertEquals("CABO DE SANTO AGOSTINHO", estabelecimento.getEndereco().getMunicipio());
        assertEquals("260290", estabelecimento.getEndereco().getCodigoMunicipio());
        assertEquals("PE", estabelecimento.getEndereco().getUf());
        assertEquals("RUA", estabelecimento.getEndereco().getTipoLogradouro());
        assertEquals("21 DE ABRIL", estabelecimento.getEndereco().getNomeLogradouro());
        assertEquals("54580530", estabelecimento.getEndereco().getCep());
        assertNull(estabelecimento.getEndereco().getNumero());
        assertTrue(estabelecimento.getEndereco().getSemNumero());
    }

    @Test
    void carregaMedicamentosDaAnvisaDecodificandoHtmlESeparandoCnpj() throws Exception {
        Path arquivo = tempDir.resolve("anvisa.csv");
        String cabecalho = "TIPO_PRODUTO;NOME_PRODUTO;DATA_FINALIZACAO_PROCESSO;CATEGORIA_REGULATORIA;"
                + "NUMERO_REGISTRO_PRODUTO;DATA_VENCIMENTO_REGISTRO;NUMERO_PROCESSO;CLASSE_TERAPEUTICA;"
                + "EMPRESA_DETENTORA_REGISTRO;SITUACAO_REGISTRO;PRINCIPIO_ATIVO\n";
        String linha = "\"MEDICAMENTO\";\"SOLU&#199;&#195;O\";\"\";\"\";\"123\";\"\";\"\";\"\";"
                + "\"12345678000199 - EMPRESA &amp; CIA\";\"ATIVO\";\"PRINC&#205;PIO\"\n";
        Files.writeString(arquivo, cabecalho + linha, StandardCharsets.ISO_8859_1);

        MedicamentoRepository repository = mock(MedicamentoRepository.class);
        when(repository.count()).thenReturn(0L);

        ETLService service = new ETLService(null, null, null, null, null, null, repository, null);
        service.carregarMedicamentosAnvisa(arquivo.toString());

        ArgumentCaptor<Medicamento> captor = ArgumentCaptor.forClass(Medicamento.class);
        verify(repository).save(captor.capture());
        Medicamento medicamento = captor.getValue();

        assertEquals("SOLUÇÃO", medicamento.getNomeProduto());
        assertEquals("PRINCÍPIO", medicamento.getPrincipioAtivo());
        assertEquals("12345678000199", medicamento.getCnpjDetentor());
        assertEquals("EMPRESA & CIA", medicamento.getEmpresaDetentora());
    }
}
