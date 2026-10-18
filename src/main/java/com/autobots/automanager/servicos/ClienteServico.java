package com.autobots.automanager.servicos;

import java.time.Instant;
import java.util.HashSet;
import com.autobots.automanager.dto.Dados.*;
import com.autobots.automanager.dto.Respostas.*;
import com.autobots.automanager.entidades.*;
import com.autobots.automanager.modelo.*;
import com.autobots.automanager.repositorios.ClienteRepositorio;
import com.autobots.automanager.erros.Conflito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClienteServico {
    private final ClienteRepositorio repositorio;
    private final ClienteSelecionador selecionador;
    private final ClienteAtualizador atualizador;
    private final EnderecoAtualizador enderecoAtualizador;
    private final DocumentoAtualizador documentoAtualizador;
    private final TelefoneAtualizador telefoneAtualizador;

    public ClienteServico(ClienteRepositorio repositorio, ClienteSelecionador selecionador,
            ClienteAtualizador atualizador, EnderecoAtualizador enderecoAtualizador,
            DocumentoAtualizador documentoAtualizador, TelefoneAtualizador telefoneAtualizador) {
        this.repositorio = repositorio; this.selecionador = selecionador; this.atualizador = atualizador;
        this.enderecoAtualizador = enderecoAtualizador; this.documentoAtualizador = documentoAtualizador;
        this.telefoneAtualizador = telefoneAtualizador;
    }

    public Page<ClienteResposta> listar(int pagina, int tamanho) {
        return repositorio.findAll(PageRequest.of(pagina, tamanho, Sort.by("id"))).map(ClienteResposta::de);
    }
    public ClienteResposta obter(long id) { return ClienteResposta.de(selecionador.selecionar(id)); }

    @Transactional
    public ClienteResposta cadastrar(ClienteCadastro dados) {
        var cliente = new Cliente();
        atualizador.atualizar(cliente, new ClienteDados(dados.nome(), dados.nomeSocial(), dados.dataNascimento()));
        cliente.setDataCadastro(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        if (dados.endereco() != null) {
            var endereco = new Endereco();
            enderecoAtualizador.atualizar(endereco, dados.endereco());
            cliente.setEndereco(endereco);
        }
        for (var dadosDocumento : dados.documentos()) {
            var documento = new Documento();
            documentoAtualizador.atualizar(documento, dadosDocumento);
            cliente.getDocumentos().add(documento);
        }
        var numeros = new HashSet<String>();
        for (var dadosTelefone : dados.telefones()) {
            if (!numeros.add(dadosTelefone.ddd() + dadosTelefone.numero()))
                throw new Conflito("Telefone já cadastrado para este cliente.");
            var telefone = new Telefone();
            telefoneAtualizador.atualizar(telefone, dadosTelefone);
            cliente.getTelefones().add(telefone);
        }
        return ClienteResposta.de(repositorio.saveAndFlush(cliente));
    }

    @Transactional
    public ClienteResposta atualizar(long id, ClienteDados dados) {
        var cliente = selecionador.bloquear(id);
        atualizador.atualizar(cliente, dados);
        return ClienteResposta.de(cliente);
    }

    @Transactional
    public void excluir(long id) { repositorio.delete(selecionador.bloquear(id)); }
}
