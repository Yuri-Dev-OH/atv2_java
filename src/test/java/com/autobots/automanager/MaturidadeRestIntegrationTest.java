package com.autobots.automanager;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.autobots.automanager.repositorios.ClienteRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica os níveis do Richardson Maturity Model exigidos na ATVII.
 *
 * <p>Os níveis 0 e 1 (HTTP e URI por recurso) também são exercitados pelos CRUDs de {@link CrudIntegrationTest}. Aqui
 * ficam a semântica HTTP do nível 2 e a navegação hipermídia (HATEOAS) do nível 3.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaturidadeRestIntegrationTest {
    static final String HOST = "http://localhost";
    static final Map<String, Object> CLIENTE = Map.of("nome", "Maria Silva", "dataNascimento", "1995-04-10");
    static final Map<String, Object> ENDERECO = Map.of("cidade", "São José dos Campos", "rua", "Rua Um", "numero", "10");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ClienteRepositorio clientes;

    @BeforeEach void limpar() { clientes.deleteAll(); }

    JsonNode enviar(MockHttpServletRequestBuilder request, Object body, int esperado) throws Exception {
        var resposta = mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
            .andExpect(status().is(esperado)).andReturn().getResponse();
        return json.readTree(resposta.getContentAsByteArray());
    }
    JsonNode obter(String uri) throws Exception {
        return json.readTree(mvc.perform(get(uri)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }
    JsonNode formulario(String uri) throws Exception {
        var resposta = mvc.perform(get(uri).accept(MediaTypes.HAL_FORMS_JSON)).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_FORMS_JSON)).andReturn().getResponse();
        return json.readTree(resposta.getContentAsByteArray());
    }
    /** Lê o href de uma relação, descartando variáveis de template como {@code {?pagina,tamanho}}. */
    static String link(JsonNode recurso, String relacao) {
        var href = recurso.at("/_links/" + relacao + "/href");
        assertThat(href.isTextual()).as("link '%s' em %s", relacao, recurso).isTrue();
        return href.asText().replaceAll("\\{.*}", "");
    }

    @Test void raizEhOPontoDeEntradaDaApi() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
            .andExpect(jsonPath("$._links.self.href").value(HOST + "/"))
            .andExpect(jsonPath("$._links.clientes.href").value(HOST + "/clientes{?pagina,tamanho}"))
            .andExpect(jsonPath("$._links.clientes.templated").value(true));
    }

    @Test void clienteCompletoEhGerenciadoApenasSeguindoLinks() throws Exception {
        String colecao = link(obter("/"), "clientes");
        var cliente = enviar(post(colecao), CLIENTE, 201);
        String self = link(cliente, "self");
        assertThat(self).isEqualTo(HOST + "/clientes/" + cliente.get("id").asLong());
        assertThat(link(cliente, "clientes")).isEqualTo(HOST + "/clientes");
        assertThat(link(cliente, "endereco")).isEqualTo(self + "/endereco");
        assertThat(link(cliente, "documentos")).isEqualTo(self + "/documentos");
        assertThat(link(cliente, "telefones")).isEqualTo(self + "/telefones");

        var documento = enviar(post(link(cliente, "documentos")), Map.of("tipo", "RG", "numero", "123456"), 201);
        assertThat(link(documento, "cliente")).isEqualTo(self);
        assertThat(link(documento, "documentos")).isEqualTo(link(cliente, "documentos"));
        var alterado = enviar(put(link(documento, "self")), Map.of("tipo", "CNH", "numero", "123456"), 200);
        assertThat(alterado.get("tipo").asText()).isEqualTo("CNH");
        var documentos = obter(link(documento, "documentos"));
        assertThat(link(documentos.at("/_embedded/documentos/0"), "self")).isEqualTo(link(documento, "self"));
        assertThat(link(documentos, "cliente")).isEqualTo(self);

        var telefone = enviar(post(link(cliente, "telefones")), Map.of("ddd", "12", "numero", "999998888"), 201);
        assertThat(link(telefone, "cliente")).isEqualTo(self);
        assertThat(link(obter(link(telefone, "telefones")).at("/_embedded/telefones/0"), "self")).isEqualTo(link(telefone, "self"));

        var endereco = enviar(post(link(cliente, "endereco")), ENDERECO, 201);
        assertThat(link(endereco, "self")).isEqualTo(link(cliente, "endereco"));
        assertThat(link(endereco, "cliente")).isEqualTo(self);

        var atualizado = obter(link(endereco, "cliente"));
        assertThat(atualizado.at("/documentos/0/tipo").asText()).isEqualTo("CNH");
        assertThat(atualizado.at("/telefones/0/numero").asText()).isEqualTo("999998888");
        assertThat(atualizado.at("/endereco/cidade").asText()).isEqualTo("São José dos Campos");

        mvc.perform(delete(link(telefone, "self"))).andExpect(status().isNoContent());
        mvc.perform(delete(link(documento, "self"))).andExpect(status().isNoContent());
        mvc.perform(delete(link(endereco, "self"))).andExpect(status().isNoContent());
        mvc.perform(delete(self)).andExpect(status().isNoContent());
        mvc.perform(get(self)).andExpect(status().isNotFound());
    }

    @Test void locationDaCriacaoApontaParaOProprioRecurso() throws Exception {
        long id = enviar(post("/clientes"), CLIENTE, 201).get("id").asLong();
        String base = "/clientes/" + id;
        for (var criacao : Map.of(base + "/documentos", Map.of("tipo", "RG", "numero", "1"),
                base + "/telefones", Map.of("ddd", "12", "numero", "99999888"), base + "/endereco", ENDERECO).entrySet()) {
            var resposta = mvc.perform(post(criacao.getKey()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(criacao.getValue()))).andExpect(status().isCreated()).andReturn().getResponse();
            var corpo = json.readTree(resposta.getContentAsByteArray());
            assertThat(resposta.getHeader("Location")).startsWith(HOST + criacao.getKey()).isEqualTo(link(corpo, "self"));
        }
    }

    @Test void paginacaoOfereceLinksDeNavegacao() throws Exception {
        for (int i = 0; i < 3; i++) enviar(post("/clientes"), CLIENTE, 201);
        String pagina = HOST + "/clientes?pagina=%d&tamanho=1";
        var meio = obter("/clientes?pagina=1&tamanho=1");
        assertThat(link(meio, "self")).isEqualTo(pagina.formatted(1));
        assertThat(link(meio, "first")).isEqualTo(pagina.formatted(0));
        assertThat(link(meio, "prev")).isEqualTo(pagina.formatted(0));
        assertThat(link(meio, "next")).isEqualTo(pagina.formatted(2));
        assertThat(link(meio, "last")).isEqualTo(pagina.formatted(2));
        assertThat(meio.at("/page/totalElements").asLong()).isEqualTo(3);
        assertThat(meio.at("/page/number").asInt()).isEqualTo(1);
        assertThat(link(meio.at("/_embedded/clientes/0"), "self")).startsWith(HOST + "/clientes/");

        var primeira = obter("/clientes?pagina=0&tamanho=1");
        assertThat(primeira.at("/_links/prev").isMissingNode()).isTrue();
        var ultima = obter(link(primeira, "last"));
        assertThat(ultima.at("/_links/next").isMissingNode()).isTrue();
        assertThat(link(obter(link(ultima, "prev")), "self")).isEqualTo(pagina.formatted(1));
    }

    @Test void colecaoVaziaMantemLinksSemEmbedded() throws Exception {
        long id = enviar(post("/clientes"), CLIENTE, 201).get("id").asLong();
        var vazia = obter("/clientes/" + id + "/telefones");
        assertThat(vazia.has("_embedded")).isFalse();
        assertThat(link(vazia, "self")).isEqualTo(HOST + "/clientes/" + id + "/telefones");
        assertThat(link(vazia, "cliente")).isEqualTo(HOST + "/clientes/" + id);
    }

    @Test void halFormsAnunciaAcoesConformeOEstadoDoCliente() throws Exception {
        long id = enviar(post("/clientes"), CLIENTE, 201).get("id").asLong();
        String uri = "/clientes/" + id;
        var semEndereco = formulario(uri);
        assertThat(semEndereco.at("/_templates/default/method").asText()).isEqualTo("PUT");
        assertThat(semEndereco.at("/_templates/excluir/method").asText()).isEqualTo("DELETE");
        assertThat(semEndereco.at("/_templates/cadastrar/method").asText()).isEqualTo("POST");
        assertThat(semEndereco.at("/_templates/cadastrar/target").asText()).isEqualTo(HOST + uri + "/endereco");

        enviar(post(uri + "/endereco"), ENDERECO, 201);
        var comEndereco = formulario(uri);
        assertThat(comEndereco.at("/_templates/cadastrar").isMissingNode()).isTrue();
        var endereco = formulario(uri + "/endereco");
        assertThat(endereco.at("/_templates/default/method").asText()).isEqualTo("PUT");
        assertThat(endereco.at("/_templates/excluir/method").asText()).isEqualTo("DELETE");
    }

    @Test void halFormsDescreveCamposDasEntradas() throws Exception {
        long id = enviar(post("/clientes"), CLIENTE, 201).get("id").asLong();
        var documentos = formulario("/clientes/" + id + "/documentos");
        assertThat(documentos.at("/_templates/default/method").asText()).isEqualTo("POST");
        var campos = documentos.at("/_templates/default/properties");
        assertThat(campos.findValuesAsText("name")).containsExactlyInAnyOrder("tipo", "numero");
        for (var campo : campos) {
            assertThat(campo.get("required").asBoolean()).isTrue();
            assertThat(campo.get("type").asText()).isEqualTo("text");
            assertThat(campo.has("readOnly")).isFalse();
        }
        var raiz = formulario("/");
        assertThat(raiz.at("/_templates/default/method").asText()).isEqualTo("POST");
        assertThat(raiz.at("/_templates/default/target").asText()).isEqualTo(HOST + "/clientes");
        var nome = raiz.at("/_templates/default/properties").findParents("name").stream()
            .filter(it -> it.get("name").asText().equals("nome")).findFirst().orElseThrow();
        assertThat(nome.get("maxLength").asInt()).isEqualTo(150);
        var telefones = formulario("/clientes/" + id + "/telefones").at("/_templates/default/properties");
        assertThat(telefones.findValuesAsText("regex")).contains("[1-9][0-9]", "[0-9]{8,9}");
    }

    @Test void errosUsamProblemDetail() throws Exception {
        mvc.perform(get("/clientes/99999999")).andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.title").value("Recurso não encontrado."))
            .andExpect(jsonPath("$.detail").value("Cliente não encontrado.")).andExpect(jsonPath("$.instance").value("/clientes/99999999"));
        mvc.perform(get("/clientes/99999999").accept(MediaTypes.HAL_FORMS_JSON)).andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detalhes.length()").value(2));
        long id = enviar(post("/clientes"), CLIENTE, 201).get("id").asLong();
        enviar(post("/clientes/" + id + "/endereco"), ENDERECO, 201);
        var conflito = enviar(post("/clientes/" + id + "/endereco"), ENDERECO, 409);
        assertThat(conflito.get("title").asText()).isEqualTo("Conflito.");
    }

    @Test void metodosEMidiasSeguemASemanticaHttp() throws Exception {
        long id = enviar(post("/clientes"), CLIENTE, 201).get("id").asLong();
        String uri = "/clientes/" + id;
        mvc.perform(patch(uri).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("PUT")))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post("/clientes").contentType(MediaType.TEXT_PLAIN).content("x")).andExpect(status().isUnsupportedMediaType());
        mvc.perform(get(uri).accept(MediaType.APPLICATION_XML)).andExpect(status().isNotAcceptable());
        // O MockMvc não descarta o corpo do HEAD; isso é feito pelo contêiner servlet (ver scripts/demo.ps1).
        mvc.perform(head(uri)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON));
        mvc.perform(options(uri)).andExpect(status().isOk())
            .andExpect(header().string("Allow", org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("GET"),
                org.hamcrest.Matchers.containsString("PUT"), org.hamcrest.Matchers.containsString("DELETE"))));
        mvc.perform(get("/rota-inexistente")).andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get(uri).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
            .andExpect(jsonPath("$._links.self.href").value(HOST + uri));
    }
}
