package com.retailmanager.rmpaydashboard.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @NoArgsConstructor @Setter @AllArgsConstructor
public class PaymentData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int paymentId;
    @Column(nullable = false)
    private String token;
    @Column(nullable = false)
    private String expDate;
    @Column(nullable = false)
    private String nameOnCard;
    @Column(nullable = false)
    private String cvn;
    @Column(nullable = false)
    private String last4Digits;
    //Cómo le seteo un vaor por default? 
    @Column(nullable = true)
    private boolean usingAutomaticPayment;
    /** Zip code enviado a BlackStone al crear el token; se reutiliza en los cobros con token. */
    @Column(nullable = true, length = 10)
    private String zipCode;
}
