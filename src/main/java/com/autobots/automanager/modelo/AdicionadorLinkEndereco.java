package com.autobots.automanager.modelo;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.autobots.automanager.controles.ClienteControle;
import com.autobots.automanager.controles.EnderecoControle;
import com.autobots.automanager.dto.Respostas.EnderecoResposta;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/** Adiciona links HATEOAS ao endereço de um cliente. */
@Component
public class AdicionadorLinkEndereco {

    /**
     * Envolve o endereço com seus links.
     *
     * @param clienteId identificador do cliente dono do endereço
     * @param endereco representação do endereço
     * @return endereço com links {@code self} (com ações de atualizar e excluir) e {@code cliente}
     */
    public EntityModel<EnderecoResposta> adicionarLink(long clienteId, EnderecoResposta endereco) {
        return EntityModel.of(endereco,
            linkTo(methodOn(EnderecoControle.class).obter(clienteId)).withSelfRel()
                .andAffordance(afford(methodOn(EnderecoControle.class).atualizar(clienteId, null)))
                .andAffordance(afford(methodOn(EnderecoControle.class).excluir(clienteId))),
            linkTo(methodOn(ClienteControle.class).obter(clienteId)).withRel("cliente"));
    }
}
