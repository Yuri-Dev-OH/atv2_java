package com.autobots.automanager.dto;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

/**
 * Entradas validadas da API.
 *
 * <p>Textos usam {@link Length} em vez de {@code @Size}: a validação é a mesma, mas o HAL-FORMS publica
 * {@code maxLength} em vez de tratar o campo como intervalo numérico ({@code type: range}).
 */
public final class Dados {
    private Dados() {}
    private static String limpar(String valor) { return valor == null ? null : valor.strip(); }

    public record ClienteCadastro(
        @NotBlank @Length(max = 150) String nome,
        @Length(max = 150) String nomeSocial,
        @NotNull @PastOrPresent LocalDate dataNascimento,
        @Valid EnderecoDados endereco,
        List<@NotNull @Valid DocumentoDados> documentos,
        List<@NotNull @Valid TelefoneDados> telefones
    ) {
        public ClienteCadastro {
            nome = limpar(nome); nomeSocial = limpar(nomeSocial);
            documentos = documentos == null ? List.of() : documentos;
            telefones = telefones == null ? List.of() : telefones;
        }
    }
    public record ClienteDados(
        @NotBlank @Length(max = 150) String nome,
        @Length(max = 150) String nomeSocial,
        @NotNull @PastOrPresent LocalDate dataNascimento
    ) {
        public ClienteDados { nome = limpar(nome); nomeSocial = limpar(nomeSocial); }
    }
    public record EnderecoDados(
        @Length(max = 100) String estado,
        @NotBlank @Length(max = 100) String cidade,
        @Length(max = 100) String bairro,
        @NotBlank @Length(max = 150) String rua,
        @NotBlank @Length(max = 20) String numero,
        @Pattern(regexp = "[0-9]{8}", message = "deve conter 8 dígitos") String codigoPostal,
        @Length(max = 255) String informacoesAdicionais
    ) {
        public EnderecoDados {
            estado = limpar(estado); cidade = limpar(cidade); bairro = limpar(bairro);
            rua = limpar(rua); numero = limpar(numero); codigoPostal = limpar(codigoPostal);
            informacoesAdicionais = limpar(informacoesAdicionais);
        }
    }
    public record DocumentoDados(
        @NotBlank @Length(max = 30) String tipo,
        @NotBlank @Length(max = 50) String numero
    ) {
        public DocumentoDados { tipo = limpar(tipo); numero = limpar(numero); }
    }
    public record TelefoneDados(
        @NotNull @Pattern(regexp = "[1-9][0-9]", message = "deve conter 2 dígitos, sem zero inicial") String ddd,
        @NotNull @Pattern(regexp = "[0-9]{8,9}", message = "deve conter 8 ou 9 dígitos") String numero
    ) {
        public TelefoneDados { ddd = limpar(ddd); numero = limpar(numero); }
    }
}
