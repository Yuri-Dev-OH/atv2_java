package com.autobots.automanager.modelo;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import com.autobots.automanager.controles.ClienteControle;
import com.autobots.automanager.controles.DocumentoControle;
import com.autobots.automanager.dto.Respostas.DocumentoResposta;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/** Adiciona links HATEOAS aos documentos de um cliente. */
@Component
public class AdicionadorLinkDocumento {

    /**
     * Envolve um documento com seus links.
     *
     * @param clienteId identificador do cliente dono do documento
     * @param documento representação do documento
     * @return documento com links {@code self} (com ações de atualizar e excluir), {@code documentos} e {@code cliente}
     */
    public EntityModel<DocumentoResposta> adicionarLink(long clienteId, DocumentoResposta documento) {
        return EntityModel.of(documento,
            linkTo(methodOn(DocumentoControle.class).obter(clienteId, documento.id())).withSelfRel()
                .andAffordance(afford(methodOn(DocumentoControle.class).atualizar(clienteId, documento.id(), null)))
                .andAffordance(afford(methodOn(DocumentoControle.class).excluir(clienteId, documento.id()))),
            linkTo(methodOn(DocumentoControle.class).listar(clienteId)).withRel("documentos"),
            linkTo(methodOn(ClienteControle.class).obter(clienteId)).withRel("cliente"));
    }

    /**
     * Envolve a lista de documentos de um cliente.
     *
     * @param clienteId identificador do cliente dono dos documentos
     * @param documentos documentos do cliente
     * @return coleção com links {@code self} (com ação de cadastrar) e {@code cliente}
     */
    public CollectionModel<EntityModel<DocumentoResposta>> adicionarLink(long clienteId, List<DocumentoResposta> documentos) {
        return CollectionModel.of(documentos.stream().map(documento -> adicionarLink(clienteId, documento)).toList(),
            linkTo(methodOn(DocumentoControle.class).listar(clienteId)).withSelfRel()
                .andAffordance(afford(methodOn(DocumentoControle.class).cadastrar(clienteId, null))),
            linkTo(methodOn(ClienteControle.class).obter(clienteId)).withRel("cliente"));
    }
}
