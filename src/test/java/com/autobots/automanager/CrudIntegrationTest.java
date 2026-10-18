package com.autobots.automanager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.autobots.automanager.repositorios.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrudIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ClienteRepositorio clientes;
    @Autowired DocumentoRepositorio documentos;
    @Autowired TelefoneRepositorio telefones;
    @Autowired EnderecoRepositorio enderecos;
    @Autowired JdbcTemplate jdbc;

    static final Map<String, Object> CLIENTE = Map.of("nome", "Maria Silva", "nomeSocial", "Maria", "dataNascimento", "1995-04-10");
    static final Map<String, Object> ENDERECO = Map.of("estado", "São Paulo", "cidade", "São José dos Campos", "bairro", "Centro",
        "rua", "Rua Um", "numero", "10", "codigoPostal", "12210000", "informacoesAdicionais", "Casa");

    @BeforeEach void limpar() { clientes.deleteAll(); }

    JsonNode enviar(MockHttpServletRequestBuilder request, Object body, int esperado) throws Exception {
        MvcResult resposta = mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
            .andExpect(status().is(esperado)).andReturn();
        return json.readTree(resposta.getResponse().getContentAsByteArray());
    }
    long cliente() throws Exception { return enviar(post("/clientes"), CLIENTE, 201).get("id").asLong(); }
    String base(long cliente, String recurso) { return "/clientes/" + cliente + "/" + recurso; }
    Map<String, Object> item(String recurso, boolean alterado) {
        return recurso.equals("documentos") ? Map.of("tipo", alterado ? "CNH" : "RG", "numero", alterado ? "987654" : "123456")
            : Map.of("ddd", alterado ? "21" : "12", "numero", alterado ? "988887777" : "999998888");
    }

    @Test void crudClientePreservaCadastroEVinculos() throws Exception {
        var cadastro = new java.util.HashMap<String, Object>(CLIENTE);
        cadastro.put("endereco", ENDERECO);
        cadastro.put("documentos", List.of(item("documentos", false)));
        cadastro.put("telefones", List.of(item("telefones", false)));
        var criado = enviar(post("/clientes"), cadastro, 201);
        long id = criado.get("id").asLong();
        assertThat(criado.get("dataCadastro").asText()).isNotBlank();
        mvc.perform(get("/clientes/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Maria Silva"));
        var atualizado = enviar(put("/clientes/{id}", id), Map.of("nome", " Maria Souza ", "dataNascimento", "1995-04-10"), 200);
        assertThat(atualizado.get("nome").asText()).isEqualTo("Maria Souza");
        assertThat(atualizado.get("nomeSocial").isNull()).isTrue();
        assertThat(atualizado.get("dataCadastro")).isEqualTo(criado.get("dataCadastro"));
        assertThat(atualizado.get("endereco")).isEqualTo(criado.get("endereco"));
        assertThat(atualizado.get("documentos")).isEqualTo(criado.get("documentos"));
        assertThat(atualizado.get("telefones")).isEqualTo(criado.get("telefones"));
        mvc.perform(delete("/clientes/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/clientes/{id}", id)).andExpect(status().isNotFound());
        assertThat(clientes.count()).isZero();
        assertThat(documentos.count()).isZero();
        assertThat(telefones.count()).isZero();
        assertThat(enderecos.count()).isZero();
    }

    @Test void listaClientesComPaginacaoEOrdenacao() throws Exception {
        long primeiro = cliente(); cliente();
        mvc.perform(get("/clientes?pagina=0&tamanho=1")).andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.clientes.length()").value(1)).andExpect(jsonPath("$._embedded.clientes[0].id").value(primeiro))
            .andExpect(jsonPath("$.page.totalElements").value(2)).andExpect(jsonPath("$.page.totalPages").value(2));
        mvc.perform(get("/clientes?pagina=9&tamanho=1")).andExpect(status().isOk()).andExpect(jsonPath("$._embedded").doesNotExist())
            .andExpect(jsonPath("$.page.number").value(9));
    }

    @ParameterizedTest @ValueSource(strings = {"documentos", "telefones"})
    void crudDosFilhos(String recurso) throws Exception {
        long cliente = cliente();
        String uri = base(cliente, recurso);
        var criado = enviar(post(uri), item(recurso, false), 201);
        long id = criado.get("id").asLong();
        mvc.perform(get(uri)).andExpect(status().isOk()).andExpect(jsonPath("$._embedded." + recurso + ".length()").value(1));
        mvc.perform(get(uri + "/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        var atualizado = enviar(put(uri + "/" + id), item(recurso, true), 200);
        assertThat(atualizado.get("id")).isEqualTo(criado.get("id"));
        assertThat(atualizado.get("numero").asText()).isEqualTo(item(recurso, true).get("numero"));
        mvc.perform(get("/clientes/{id}", cliente)).andExpect(jsonPath("$." + recurso + "[0].numero").value(item(recurso, true).get("numero")));
        mvc.perform(delete(uri + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(uri + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete(uri + "/" + id)).andExpect(status().isNotFound());
        assertThat(recurso.equals("documentos") ? documentos.count() : telefones.count()).isZero();
        assertThat(clientes.count()).isEqualTo(1);
    }

    @Test void crudEnderecoIncluiCepERemocaoDosCamposOpcionais() throws Exception {
        long cliente = cliente(); String uri = base(cliente, "endereco");
        mvc.perform(get(uri)).andExpect(status().isNotFound());
        var criado = enviar(post(uri), ENDERECO, 201);
        enviar(post(uri), ENDERECO, 409);
        var novo = new java.util.HashMap<String, Object>(ENDERECO);
        novo.put("codigoPostal", "01311000"); novo.remove("informacoesAdicionais");
        var atualizado = enviar(put(uri), novo, 200);
        assertThat(atualizado.get("id")).isEqualTo(criado.get("id"));
        assertThat(atualizado.get("informacoesAdicionais").isNull()).isTrue();
        mvc.perform(get(uri)).andExpect(status().isOk()).andExpect(jsonPath("$.codigoPostal").value("01311000"));
        mvc.perform(delete(uri)).andExpect(status().isNoContent());
        assertThat(enderecos.count()).isZero();
        mvc.perform(get("/clientes/{id}", cliente)).andExpect(jsonPath("$.endereco").isEmpty());
        enviar(put(uri), ENDERECO, 404);
        mvc.perform(delete(uri)).andExpect(status().isNotFound());
        enviar(post(uri), ENDERECO, 201);
    }

    @ParameterizedTest @ValueSource(strings = {"documentos", "telefones"})
    void naoPermiteAcessarFilhoDeOutroCliente(String recurso) throws Exception {
        long dono = cliente(), outro = cliente();
        long id = enviar(post(base(dono, recurso)), item(recurso, false), 201).get("id").asLong();
        String indevida = base(outro, recurso) + "/" + id;
        mvc.perform(get(indevida)).andExpect(status().isNotFound());
        enviar(put(indevida), item(recurso, true), 404);
        mvc.perform(delete(indevida)).andExpect(status().isNotFound());
        mvc.perform(get(base(dono, recurso) + "/" + id)).andExpect(jsonPath("$.numero").value(item(recurso, false).get("numero")));
    }

    @ParameterizedTest @ValueSource(strings = {"documentos", "telefones", "endereco"})
    void clienteAusenteNaoCriaFilhos(String recurso) throws Exception {
        String uri = base(99999999, recurso);
        mvc.perform(get(uri)).andExpect(status().isNotFound());
        enviar(post(uri), recurso.equals("endereco") ? ENDERECO : item(recurso, false), 404);
        if (recurso.equals("endereco")) {
            enviar(put(uri), ENDERECO, 404);
            mvc.perform(delete(uri)).andExpect(status().isNotFound());
        }
    }

    @Test void documentoDuplicadoDesfazCadastroInteiro() throws Exception {
        long dono = cliente();
        enviar(post(base(dono, "documentos")), item("documentos", false), 201);
        var dados = new java.util.HashMap<String, Object>(CLIENTE);
        dados.put("endereco", ENDERECO); dados.put("telefones", List.of(item("telefones", false)));
        dados.put("documentos", List.of(item("documentos", false)));
        enviar(post("/clientes"), dados, 409);
        assertThat(clientes.count()).isEqualTo(1);
        assertThat(documentos.count()).isEqualTo(1);
        assertThat(enderecos.count()).isZero();
        assertThat(telefones.count()).isZero();
    }

    @Test void documentoDuplicadoNaAtualizacaoPreservaDadosAnteriores() throws Exception {
        long dono = cliente(); String uri = base(dono, "documentos");
        enviar(post(uri), item("documentos", false), 201);
        long segundo = enviar(post(uri), item("documentos", true), 201).get("id").asLong();
        enviar(put(uri + "/" + segundo), item("documentos", false), 409);
        mvc.perform(get(uri + "/" + segundo)).andExpect(jsonPath("$.numero").value("987654"));
    }

    @Test void telefoneNaoDuplicaNoMesmoClienteMasPodeSerCompartilhadoEntreClientes() throws Exception {
        long dono = cliente(); String uri = base(dono, "telefones");
        long primeiro = enviar(post(uri), item("telefones", false), 201).get("id").asLong();
        enviar(post(uri), item("telefones", false), 409);
        enviar(put(uri + "/" + primeiro), item("telefones", false), 200);
        long segundo = enviar(post(uri), item("telefones", true), 201).get("id").asLong();
        enviar(put(uri + "/" + segundo), item("telefones", false), 409);
        enviar(post(base(cliente(), "telefones")), item("telefones", false), 201);
    }

    @Test void telefoneDuplicadoNoCadastroInicialNaoGravaCliente() throws Exception {
        var dados = new java.util.HashMap<String, Object>(CLIENTE);
        dados.put("telefones", List.of(item("telefones", false), item("telefones", false)));
        enviar(post("/clientes"), dados, 409);
        assertThat(clientes.count()).isZero();
    }

    @Test void comparaIdsGrandesPorValor() throws Exception {
        jdbc.execute("ALTER TABLE telefone ALTER COLUMN id RESTART WITH 1000");
        long cliente = cliente(); String uri = base(cliente, "telefones");
        long id = enviar(post(uri), item("telefones", false), 201).get("id").asLong();
        assertThat(id).isGreaterThanOrEqualTo(1000);
        enviar(put(uri + "/" + id), item("telefones", true), 200);
        mvc.perform(delete(uri + "/" + id)).andExpect(status().isNoContent());
    }

    @Test void cadastrosConcorrentesNaoDuplicamTelefone() throws Exception {
        long cliente = cliente(); String uri = base(cliente, "telefones");
        var inicio = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> tarefa = () -> {
                inicio.await(10, java.util.concurrent.TimeUnit.SECONDS);
                return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(item("telefones", false)))).andReturn().getResponse().getStatus();
            };
            var a = executor.submit(tarefa); var b = executor.submit(tarefa);
            assertThat(List.of(a.get(15, java.util.concurrent.TimeUnit.SECONDS), b.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(telefones.count()).isEqualTo(1);
    }

    static Stream<Arguments> entradasInvalidas() {
        return Stream.of(
            Arguments.of("/clientes", "{}"),
            Arguments.of("/clientes", "{\"nome\":\" \",\"dataNascimento\":\"1990-01-01\"}"),
            Arguments.of("/clientes", "{\"nome\":\"Ana\",\"dataNascimento\":\"2999-01-01\"}"),
            Arguments.of("/clientes", "{\"nome\":\"Ana\",\"dataNascimento\":\"1990-02-30\"}"),
            Arguments.of("/clientes", "{\"nome\":\"Ana\",\"dataNascimento\":\"1990-01-01\",\"id\":10}"),
            Arguments.of("/clientes", "{\"nome\":\"Ana\",\"dataNascimento\":\"1990-01-01\",\"dataCadastro\":\"2000-01-01\"}"),
            Arguments.of("/clientes", "{\"nome\":\"Ana\",\"dataNascimento\":\"1990-01-01\",\"documentos\":[null]}"),
            Arguments.of("/clientes", "{\"nome\":\"Ana\",\"dataNascimento\":\"1990-01-01\",\"endereco\":{}}"),
            Arguments.of("/clientes", "{\"nome\":123,\"dataNascimento\":\"1990-01-01\"}"),
            Arguments.of("/clientes", "null"),
            Arguments.of("/clientes", "{texto"),
            Arguments.of("/clientes", "{} {}"));
    }
    @ParameterizedTest @MethodSource("entradasInvalidas")
    void rejeitaDadosInvalidosSemPersistir(String uri, String body) throws Exception {
        mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").isNotEmpty());
        assertThat(clientes.count()).isZero();
    }

    @Test void validaDadosDosFilhos() throws Exception {
        long cliente = cliente();
        enviar(post(base(cliente, "documentos")), Map.of("tipo", " ", "numero", "123"), 400);
        enviar(post(base(cliente, "telefones")), Map.of("ddd", "012", "numero", "telefone"), 400);
        var endereco = new java.util.HashMap<String, Object>(ENDERECO); endereco.put("codigoPostal", "123");
        enviar(post(base(cliente, "endereco")), endereco, 400);
        assertThat(documentos.count()).isZero(); assertThat(telefones.count()).isZero(); assertThat(enderecos.count()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"/clientes/0", "/clientes/-1", "/clientes/abc", "/clientes?pagina=-1", "/clientes?tamanho=0", "/clientes?tamanho=101"})
    void validaParametros(String uri) throws Exception { mvc.perform(get(uri)).andExpect(status().isBadRequest()); }

    @Test void retorna404ParaClienteAusente() throws Exception {
        mvc.perform(get("/clientes/99999999")).andExpect(status().isNotFound());
        enviar(put("/clientes/99999999"), CLIENTE, 404);
        mvc.perform(delete("/clientes/99999999")).andExpect(status().isNotFound());
    }

    @Test void respostasDeCriacaoPossuemLocation() throws Exception {
        var resposta = mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(CLIENTE)))
            .andExpect(status().isCreated()).andReturn().getResponse();
        var corpo = json.readTree(resposta.getContentAsByteArray());
        assertThat(resposta.getHeader("Location")).isEqualTo("http://localhost/clientes/" + corpo.get("id").asLong())
            .isEqualTo(corpo.at("/_links/self/href").asText());
    }
}
