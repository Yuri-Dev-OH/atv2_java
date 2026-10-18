package com.autobots.automanager.modelo;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import com.autobots.automanager.controles.ClienteControle;
import com.autobots.automanager.controles.TelefoneControle;
import com.autobots.automanager.dto.Respostas.TelefoneResposta;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/** Adiciona links HATEOAS aos telefones de um cliente. */
@Component
public class AdicionadorLinkTelefone {

    /**
     * Envolve um telefone com seus links.
     *
     * @param clienteId identificador do cliente dono do telefone
     * @param telefone representação do telefone
     * @return telefone com links {@code self} (com ações de atualizar e excluir), {@code telefones} e {@code cliente}
     */
    public EntityModel<TelefoneResposta> adicionarLink(long clienteId, TelefoneResposta telefone) {
        return EntityModel.of(telefone,
            linkTo(methodOn(TelefoneControle.class).obter(clienteId, telefone.id())).withSelfRel()
                .andAffordance(afford(methodOn(TelefoneControle.class).atualizar(clienteId, telefone.id(), null)))
                .andAffordance(afford(methodOn(TelefoneControle.class).excluir(clienteId, telefone.id()))),
            linkTo(methodOn(TelefoneControle.class).listar(clienteId)).withRel("telefones"),
            linkTo(methodOn(ClienteControle.class).obter(clienteId)).withRel("cliente"));
    }

    /**
     * Envolve a lista de telefones de um cliente.
     *
     * @param clienteId identificador do cliente dono dos telefones
     * @param telefones telefones do cliente
     * @return coleção com links {@code self} (com ação de cadastrar) e {@code cliente}
     */
    public CollectionModel<EntityModel<TelefoneResposta>> adicionarLink(long clienteId, List<TelefoneResposta> telefones) {
        return CollectionModel.of(telefones.stream().map(telefone -> adicionarLink(clienteId, telefone)).toList(),
            linkTo(methodOn(TelefoneControle.class).listar(clienteId)).withSelfRel()
                .andAffordance(afford(methodOn(TelefoneControle.class).cadastrar(clienteId, null))),
            linkTo(methodOn(ClienteControle.class).obter(clienteId)).withRel("cliente"));
    }
}
