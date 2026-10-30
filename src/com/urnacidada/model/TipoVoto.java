package com.urnacidada.model;

public enum TipoVoto {
    VALIDO,   // número existente de um candidato
    LEGENDA,  // dois primeiros dígitos identificam o partido num cargo proporcional
    BRANCO,   // eleitor escolheu votar em branco
    NULO      // número digitado não corresponde a nenhum candidato
}
