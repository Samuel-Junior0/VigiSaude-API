# VigiSaúde API

API REST em **Java + Spring Boot** para cruzamento e análise epidemiológica de dados abertos de farmacovigilância. O sistema integra três bases públicas brasileiras, **VigiMed** (notificações de eventos adversos), **CNES** (estabelecimentos de saúde) e **ANVISA** (medicamentos registrados), e responde a uma pergunta simples:

> *"Para este sintoma, neste estado, quais medicamentos aparecem mais nas notificações e onde há estabelecimentos de saúde no meu município?"*

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Configuração](#configuração)
- [Como executar](#como-executar)
- [Endpoints](#endpoints)
- [Pipeline ETL](#pipeline-etl)
- [Modelo de dados](#modelo-de-dados)
- [Testes](#testes)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Funcionalidades

- **Carga (ETL) de dados abertos** do VigiMed (notificações, reações e medicamentos), do CNES e da ANVISA.
- **Normalização de sintomas**: termos populares (ex.: "dor de cabeça") são convertidos para o termo técnico MedDRA (ex.: `CEFALEIA`) por meio de uma tabela de sinônimos.
- **Consulta de farmacovigilância** por sintoma, município e UF, retornando:
    - total de notificações históricas para o sintoma na UF;
    - princípio ativo mais notificado e seu percentual dos casos;
    - estabelecimentos de saúde (CNES) do município informado, com endereço desmembrado.
- **Quarentena de dados inválidos**: linhas malformadas, sem identificador ou órfãs não derrubam a carga; ficam registradas em `tb_quarentena` com o motivo e a linha original.
- **Limpeza e padronização**: CEP, CNES, entidades HTML, valores nulos disfarçados ("Não informado", "None", "-") e separação de logradouro, número e tipo de logradouro.
- **Documentação interativa** com Swagger UI (springdoc-openapi).

## Tecnologias

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 23 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA) |
| Banco de dados | MariaDB |
| Documentação da API | springdoc-openapi 3.1.0 (Swagger UI) |
| Leitura de CSV | Apache Commons CSV 1.12.0 |
| Utilitários de texto | Apache Commons Text 1.12.0 |
| Boilerplate | Lombok |
| Build | Maven (wrapper incluso) |

## Arquitetura

Arquitetura em camadas, com separação entre entidades de domínio e DTOs de resposta:

```
Controller  →  Service  →  Repository  →  MariaDB
   (DTOs)      (regras)     (Spring Data JPA)
```

| Camada | Responsabilidade |
|---|---|
| `controller` | Expõe os endpoints REST (`ETLController`, `ConsultaFarmacovigilanciaController`) |
| `service` | Regras de negócio: `ETLService` (carga), `NormalizacaoService` (sinônimos), `ConsultaService` (consulta) |
| `repository` | Acesso a dados com Spring Data JPA e consultas JPQL |
| `model` | Entidades JPA |
| `dto` | Objetos de entrada e saída da API (JSON em `snake_case`) |
| `utils` | `CsvSanitizer`, `CsvLineReader`, `AddressParser`, `ParsedAddress` |

## Pré-requisitos

- **JDK 23**
- **MariaDB** em execução (porta padrão `3306`)
- Os arquivos CSV das bases abertas (veja [Pipeline ETL](#pipeline-etl))
- Maven é opcional, pois o projeto inclui o `mvnw`

## Configuração

1. Crie o banco de dados:

   ```sql
   CREATE DATABASE vigisaude CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Ajuste as credenciais em `src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:mariadb://localhost:3306/vigisaude
   spring.datasource.username=SEU_USUARIO
   spring.datasource.password=SUA_SENHA
   ```

   > **Dica de segurança:** evite versionar senhas. Use variáveis de ambiente, por exemplo
   > `spring.datasource.password=${DB_PASSWORD}`, e defina `DB_PASSWORD` antes de subir a aplicação.

O Hibernate está com `ddl-auto=update`, então as tabelas são criadas automaticamente na primeira execução. O projeto também usa *batch inserts* (`batch_size=100`) para acelerar as cargas.

## Como executar

```bash
# Clonar o repositório
git clone <url-do-repositorio>
cd VigiSaude-API

# Rodar a aplicação
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

A API sobe em `http://localhost:8080`. A documentação interativa fica em:

```
http://localhost:8080/swagger-ui.html
```

Para gerar o `.jar`:

```bash
./mvnw clean package
java -jar target/vigisaude-0.0.1-SNAPSHOT.jar
```

## Endpoints

### `POST /api/etl/executar`

Executa a carga completa das bases. Os caminhos são **caminhos de arquivos no servidor** onde a API está rodando.

| Parâmetro | Obrigatório | Descrição |
|---|:---:|---|
| `cnesPath` | ✅ | CSV de estabelecimentos do CNES |
| `municipiosPath` | ✅ | CSV de municípios (código IBGE → nome) |
| `notificacoesPath` | ✅ | CSV de notificações do VigiMed |
| `reacoesPath` | ✅ | CSV de reações do VigiMed |
| `medicamentosPath` | ✅ | CSV de medicamentos do VigiMed |
| `anvisaPath` | ❌ | CSV de medicamentos registrados na ANVISA |

Exemplo:

```bash
curl -X POST "http://localhost:8080/api/etl/executar" \
  --data-urlencode "cnesPath=/dados/cnes.csv" \
  --data-urlencode "municipiosPath=/dados/municipios.csv" \
  --data-urlencode "notificacoesPath=/dados/vigimed_notificacoes.csv" \
  --data-urlencode "reacoesPath=/dados/vigimed_reacoes.csv" \
  --data-urlencode "medicamentosPath=/dados/vigimed_medicamentos.csv" \
  --data-urlencode "anvisaPath=/dados/anvisa_medicamentos.csv"
```

Resposta: `Processo ETL executado com sucesso para todas as bases de dados!`

### `POST /api/farmacovigilancia/consultar`

Consulta estatísticas históricas e estabelecimentos de saúde.

**Requisição**

```json
{
  "sintoma": "dor de cabeça",
  "municipio": "João Pessoa",
  "uf": "PB"
}
```

**Resposta**

```json
{
  "consulta": {
    "termo_digitado": "dor de cabeça",
    "termo_padronizado": "CEFALEIA",
    "status_normalizacao": "Sinônimo identificado e convertido via tabela de equivalências MedDRA"
  },
  "estatisticas_historicas_vigimed": {
    "total_notificacoes_encontradas": 1234,
    "medicamento_mais_notificado_associado": {
      "principio_ativo": "EXEMPLO",
      "percentual_dos_casos": "12.3%"
    },
    "registros_incompletos_ignorados_na_analise": 0
  },
  "atendimento_proximo_cnes": {
    "municipio_filtrado": "João Pessoa",
    "uf": "PB",
    "estabelecimentos_encontrados": [
      {
        "cnes": "0000000",
        "nome_fantasia": "UNIDADE DE EXEMPLO",
        "tipo_unidade": "POSTO DE SAUDE",
        "endereco_desmembrado": {
          "tipo_logradouro": "RUA",
          "logradouro": "EXEMPLO",
          "numero": "100",
          "sem_numero": false,
          "bairro": "CENTRO",
          "cep": "58000000"
        }
      }
    ]
  }
}
```

> Os valores acima são ilustrativos. Se o sintoma não tiver sinônimo cadastrado, o termo é usado diretamente em caixa alta, e o campo `status_normalizacao` informa isso.

## Pipeline ETL

O `ETLController` executa as etapas abaixo, nesta ordem:

| # | Etapa | O que faz |
|---|---|---|
| 1 | `carregarSinonimos` | Popula sinônimos básicos de sintomas (ex.: "coceira" → `PRURIDO`) |
| 2 | `carregarCNES` | Lê estabelecimentos, resolve UF e município pelo código IBGE e desmembra o endereço |
| 3 | `carregarNotificacoes` | Importa notificações do VigiMed |
| 4 | `carregarReacoes` | Importa reações (termos MedDRA PT) vinculadas às notificações |
| 5 | `carregarSinonimosDeReacoes` | Gera sinônimos LLT → PT a partir do arquivo de reações |
| 6 | `carregarMedicamentos` | Importa medicamentos notificados e seus princípios ativos |
| 7 | `carregarMedicamentosAnvisa` | Importa o catálogo da ANVISA (opcional) |

**Detalhes importantes**

- **Encodings**: VigiMed é lido em `windows-1252`, CNES e ANVISA em `ISO-8859-1`, e o CSV de municípios em `UTF-8`.
- **Idempotência**: a carga do CNES é ignorada se já houver estabelecimentos no banco.
- **Tolerância a falhas**: arquivos inexistentes geram um aviso no log e a etapa é pulada, sem interromper a aplicação.
- **Quarentena**: registros problemáticos vão para `tb_quarentena` com um dos motivos:

  | Motivo | Significado |
    |---|---|
  | `LINHA_MALFORMADA` | A linha não pôde ser interpretada como CSV válido |
  | `ID_VAZIO` | A linha não tem identificador de notificação |
  | `ORFAO` | Reação ou medicamento referencia uma notificação inexistente |

## Modelo de dados

Modelagem relacional normalizada (3FN):

| Tabela | Descrição |
|---|---|
| `tb_notificacao` | Notificação (data, idade, faixa etária, sexo, UF) |
| `tb_reacao_notificada` | Reações associadas à notificação (termo MedDRA PT) |
| `tb_medicamento_notificado` | Medicamentos associados à notificação |
| `tb_principio_ativo` | Princípios ativos de cada medicamento notificado |
| `tb_medicamento` | Catálogo de medicamentos da ANVISA |
| `tb_estabelecimento` | Estabelecimentos de saúde (CNES) |
| `tb_endereco` | Endereço desmembrado do estabelecimento |
| `tb_sinonimo_termo` | Equivalências entre termo bruto e termo padronizado |
| `tb_quarentena` | Registros rejeitados durante o ETL |

## Testes

```bash
./mvnw test
```

Há testes para o `ETLService` (mapa de municípios, carga de dados), o `CsvLineReader` e os utilitários de sanitização e parsing de endereço.

## Estrutura do projeto

```
VigiSaude-API
├── pom.xml
├── mvnw / mvnw.cmd
└── src
    ├── main
    │   ├── java/br/edu/ifpb/vigisaude
    │   │   ├── VigisaudeApplication.java
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   ├── service/
    │   │   └── utils/
    │   └── resources/application.properties
    └── test/java/br/edu/ifpb/vigisaude
        ├── service/ETLServiceTest.java
        └── utils/
```

## Fontes de dados

- [VigiMed](https://dados.gov.br/): notificações de eventos adversos (ANVISA)
- [CNES](https://cnes.datasus.gov.br/): Cadastro Nacional de Estabelecimentos de Saúde
- [ANVISA](https://dados.gov.br/): medicamentos registrados
- Tabela de municípios do IBGE

---

Projeto desenvolvido no contexto acadêmico do **IFPB**.