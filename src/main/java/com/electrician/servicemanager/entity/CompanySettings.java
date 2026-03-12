package com.electrician.servicemanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "company_settings")
public class CompanySettings {

    @Id
    private Long ownerId;   // Same as User.id — one row per owner

    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String companyPhone2;   // optional second number
    private String companyEmail;
    private String gstNumber;
    private String tagline;

    // Rate card JSON stored as text: { "AC": ["Gas Filling: 500", ...], "Washing Machine": [...] }
    @Column(columnDefinition = "TEXT")
    private String rateCardJson;

    // WhatsApp message templates (owner can customize)
    @Column(columnDefinition = "TEXT")
    private String invoiceMsgTemplate;

    @Column(columnDefinition = "TEXT")
    private String assignedMsgTemplate;

    @Column(columnDefinition = "TEXT")
    private String warrantyMsgTemplate;

    @Column(columnDefinition = "TEXT")
    private String thankyouMsgTemplate;

    // Owner's handwritten signature — stored as base64 PNG (from frontend canvas)
    @Column(columnDefinition = "TEXT")
    private String signatureBase64;

    // Custom links — JSON array: [{label:"Website", url:"https://..."}, ...]
    @Column(columnDefinition = "TEXT")
    private String linksJson;
}