package com.autobots.automanager.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.autobots.automanager.entidades.*;
import org.springframework.hateoas.server.core.Relation;

/**
 * Representações devolvidas pela API.
 *
 * <p>{@link Relation} define os nomes usados pelo HAL em {@code _embedded}, evitando nomes gerados como
 * {@code documentoRespostaList}.
 */
public final class Respostas {
    private Respostas() {}
    @Relation(itemRelation = "documento", collectionRelation = "documentos")
    public record DocumentoResposta(Long id, String tipo, String numero) {
        public static DocumentoResposta de(Documento d) { return new DocumentoResposta(d.getId(), d.getTipo(), d.getNumero()); }
    }
    @Relation(itemRelation = "telefone", collectionRelation = "telefones")
    public record TelefoneResposta(Long id, String ddd, String numero) {
        public static TelefoneResposta de(Telefone t) { return new TelefoneResposta(t.getId(), t.getDdd(), t.getNumero()); }
    }
    @Relation(itemRelation = "endereco", collectionRelation = "enderecos")
    public record EnderecoResposta(Long id, String estado, String cidade, String bairro, String rua, String numero,
                                   String codigoPostal, String informacoesAdicionais) {
        public static EnderecoResposta de(Endereco e) {
            return e == null ? null : new EnderecoResposta(e.getId(), e.getEstado(), e.getCidade(), e.getBairro(),
                e.getRua(), e.getNumero(), e.getCodigoPostal(), e.getInformacoesAdicionais());
        }
    }
    @Relation(itemRelation = "cliente", collectionRelation = "clientes")
    public record ClienteResposta(Long id, String nome, String nomeSocial, LocalDate dataNascimento, Instant dataCadastro,
                                 EnderecoResposta endereco, List<DocumentoResposta> documentos, List<TelefoneResposta> telefones) {
        public static ClienteResposta de(Cliente c) {
            return new ClienteResposta(c.getId(), c.getNome(), c.getNomeSocial(), c.getDataNascimento(), c.getDataCadastro(),
                EnderecoResposta.de(c.getEndereco()), c.getDocumentos().stream().map(DocumentoResposta::de).toList(),
                c.getTelefones().stream().map(TelefoneResposta::de).toList());
        }
    }
}
