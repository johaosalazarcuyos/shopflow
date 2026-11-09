package dev.harold.shopflow.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShippingAddress {

    @Column(name = "ship_full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "ship_line1", nullable = false, length = 200)
    private String line1;

    @Column(name = "ship_district", nullable = false, length = 80)
    private String district;

    @Column(name = "ship_city", nullable = false, length = 80)
    private String city;

    @Column(name = "ship_phone", nullable = false, length = 20)
    private String phone;

    protected ShippingAddress() {
    }

    public ShippingAddress(String fullName, String line1, String district, String city, String phone) {
        this.fullName = fullName;
        this.line1 = line1;
        this.district = district;
        this.city = city;
        this.phone = phone;
    }

    public String getFullName() { return fullName; }
    public String getLine1() { return line1; }
    public String getDistrict() { return district; }
    public String getCity() { return city; }
    public String getPhone() { return phone; }
}
