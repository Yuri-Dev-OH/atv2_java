package com.autobots.automanager.modelo;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import com.autobots.automanager.controles.*;
import com.autobots.automanager.dto.Respostas.ClienteResposta;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.stereotype.Component;

/**
 * Adiciona links HATEOAS (nível 3 do RMM) às representações de cliente.
 *
 * <p>Cada cliente aponta para si, para a coleção e para seus sub-recursos. As affordances anunciam, em HAL-FORMS,
 * as ações permitidas no estado atual do recurso.
 */
@Component
public class AdicionadorLinkCliente {

    /**
     * Envolve um cliente com seus links.
     *
     * @param cliente representação do cliente
     * @return cliente com links {@code self}, {@code clientes}, {@code endereco}, {@code documentos} e {@code telefones}
     */
    public EntityModel<ClienteResposta> adicionarLink(ClienteResposta cliente) {
        long id = cliente.id();
        Link endereco = linkTo(methodOn(EnderecoControle.class).obter(id)).withRel("endereco");
        if (cliente.endereco() == null) {
            endereco = endereco.andAffordance(afford(methodOn(EnderecoControle.class).cadastrar(id, null)));
        }
        return EntityModel.of(cliente,
            linkTo(methodOn(ClienteControle.class).obter(id)).withSelfRel()
                .andAffordance(afford(methodOn(ClienteControle.class).atualizar(id, null)))
                .andAffordance(afford(methodOn(ClienteControle.class).excluir(id))),
            linkTo(methodOn(ClienteControle.class).listar(null, null)).withRel("clientes"),
            endereco,
            linkTo(methodOn(DocumentoControle.class).listar(id)).withRel("documentos"),
            linkTo(methodOn(TelefoneControle.class).listar(id)).withRel("telefones"));
    }

    /**
     * Envolve uma página de clientes com links de navegação entre páginas.
     *
     * @param pagina página consultada
     * @return modelo paginado com {@code self}, {@code first}, {@code last} e, quando aplicáveis, {@code prev} e
     *     {@code next}
     */
    public PagedModel<EntityModel<ClienteResposta>> adicionarLink(Page<ClienteResposta> pagina) {
        int numero = pagina.getNumber();
        int tamanho = pagina.getSize();
        int ultima = Math.max(pagina.getTotalPages() - 1, 0);
        var links = new ArrayList<Link>();
        links.add(linkPagina(numero, tamanho, IanaLinkRelations.SELF)
            .andAffordance(afford(methodOn(ClienteControle.class).cadastrar(null))));
        links.add(linkPagina(0, tamanho, IanaLinkRelations.FIRST));
        if (numero > 0) links.add(linkPagina(Math.min(numero - 1, ultima), tamanho, IanaLinkRelations.PREV));
        if (numero < ultima) links.add(linkPagina(numero + 1, tamanho, IanaLinkRelations.NEXT));
        links.add(linkPagina(ultima, tamanho, IanaLinkRelations.LAST));
        var metadados = new PageMetadata(tamanho, numero, pagina.getTotalElements(), pagina.getTotalPages());
        return PagedModel.of(pagina.getContent().stream().map(this::adicionarLink).toList(), metadados, links);
    }

    private Link linkPagina(int pagina, int tamanho, LinkRelation relacao) {
        return linkTo(methodOn(ClienteControle.class).listar(pagina, tamanho)).withRel(relacao);
    }
}
