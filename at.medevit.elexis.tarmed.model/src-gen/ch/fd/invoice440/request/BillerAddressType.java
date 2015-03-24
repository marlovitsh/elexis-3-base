//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.03.18 at 03:48:09 PM CET 
//


package ch.fd.invoice440.request;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for billerAddressType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="billerAddressType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="company" type="{http://www.forum-datenaustausch.ch/invoice}companyType"/>
 *         &lt;element name="person" type="{http://www.forum-datenaustausch.ch/invoice}personType"/>
 *       &lt;/choice>
 *       &lt;attribute name="ean_party" use="required" type="{http://www.forum-datenaustausch.ch/invoice}eanPartyType" />
 *       &lt;attribute name="zsr" type="{http://www.forum-datenaustausch.ch/invoice}zsrPartyType" />
 *       &lt;attribute name="specialty" type="{http://www.forum-datenaustausch.ch/invoice}stringType1_350" />
 *       &lt;attribute name="uid_number" type="{http://www.forum-datenaustausch.ch/invoice}stringType1_15" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "billerAddressType", propOrder = {
    "company",
    "person"
})
public class BillerAddressType {

    protected CompanyType company;
    protected PersonType person;
    @XmlAttribute(name = "ean_party", required = true)
    protected String eanParty;
    @XmlAttribute(name = "zsr")
    protected String zsr;
    @XmlAttribute(name = "specialty")
    protected String specialty;
    @XmlAttribute(name = "uid_number")
    protected String uidNumber;

    /**
     * Gets the value of the company property.
     * 
     * @return
     *     possible object is
     *     {@link CompanyType }
     *     
     */
    public CompanyType getCompany() {
        return company;
    }

    /**
     * Sets the value of the company property.
     * 
     * @param value
     *     allowed object is
     *     {@link CompanyType }
     *     
     */
    public void setCompany(CompanyType value) {
        this.company = value;
    }

    /**
     * Gets the value of the person property.
     * 
     * @return
     *     possible object is
     *     {@link PersonType }
     *     
     */
    public PersonType getPerson() {
        return person;
    }

    /**
     * Sets the value of the person property.
     * 
     * @param value
     *     allowed object is
     *     {@link PersonType }
     *     
     */
    public void setPerson(PersonType value) {
        this.person = value;
    }

    /**
     * Gets the value of the eanParty property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEanParty() {
        return eanParty;
    }

    /**
     * Sets the value of the eanParty property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEanParty(String value) {
        this.eanParty = value;
    }

    /**
     * Gets the value of the zsr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getZsr() {
        return zsr;
    }

    /**
     * Sets the value of the zsr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setZsr(String value) {
        this.zsr = value;
    }

    /**
     * Gets the value of the specialty property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSpecialty() {
        return specialty;
    }

    /**
     * Sets the value of the specialty property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSpecialty(String value) {
        this.specialty = value;
    }

    /**
     * Gets the value of the uidNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUidNumber() {
        return uidNumber;
    }

    /**
     * Sets the value of the uidNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUidNumber(String value) {
        this.uidNumber = value;
    }

}