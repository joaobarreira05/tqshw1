package pt.zeromonos.garbagecollection.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// @Data do Lombok cria os getters, setters, etc. por nós.
@Data
// Diz ao Jackson para ignorar quaisquer outros campos que a API devolva e que não nos interessem.
@JsonIgnoreProperties(ignoreUnknown = true)
public class MunicipioDTO {
    // Este nome de variável tem de corresponder EXATAMENTE ao nome do campo no JSON da API.
    // A API da geoapi.pt devolve um campo "nome" para o nome do município.
    private String nome;
}