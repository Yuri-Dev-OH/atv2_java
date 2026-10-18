package com.autobots.automanager;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import com.autobots.automanager.dto.Dados.*;
import com.autobots.automanager.servicos.ClienteServico;
import com.autobots.automanager.repositorios.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

class PersistenciaIntegrationTest {
    @TempDir Path pasta;

    private ConfigurableApplicationContext iniciar() {
        return new SpringApplicationBuilder(AutomanagerApplication.class).web(WebApplicationType.NONE)
            .run("--spring.datasource.url=jdbc:h2:file:" + pasta.resolve("clientes").toAbsolutePath().toString().replace('\\', '/'),
                "--spring.datasource.username=sa", "--spring.datasource.password=", "--spring.main.banner-mode=off");
    }

    @Test void conservaCadastroAposReabrirBancoEmArquivo() {
        long id;
        try (var contexto = iniciar()) {
            var servico = contexto.getBean(ClienteServico.class);
            var cliente = servico.cadastrar(new ClienteCadastro("Ana", null, LocalDate.of(1990, 1, 1),
                new EnderecoDados("SP", "São Paulo", null, "Rua A", "1", "01001000", null),
                List.of(new DocumentoDados("RG", "ABC123")), List.of(new TelefoneDados("11", "999998888"))));
            id = cliente.id();
        }
        try (var contexto = iniciar()) {
            var servico = contexto.getBean(ClienteServico.class);
            var cliente = servico.obter(id);
            assertThat(cliente.nome()).isEqualTo("Ana");
            assertThat(cliente.documentos()).hasSize(1);
            assertThat(cliente.telefones()).hasSize(1);
            assertThat(cliente.endereco().codigoPostal()).isEqualTo("01001000");
            assertThat(contexto.getBean(ClienteRepositorio.class).count()).isEqualTo(1);
            servico.excluir(id);
            assertThat(contexto.getBean(DocumentoRepositorio.class).count()).isZero();
            assertThat(contexto.getBean(TelefoneRepositorio.class).count()).isZero();
            assertThat(contexto.getBean(EnderecoRepositorio.class).count()).isZero();
        }
    }
}
