package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.annotation.RequireRoles;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateAddressRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.response.AddressResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for Address entity.
 * Handles all address-related queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@RequireRoles({"ADMIN", "CUSTOMER"})
public class AddressResolver {

    private final AddressService addressService;

    // ========================================================================
    // ADDRESS QUERIES
    // ========================================================================

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public AddressResponse address(@Argument UUID id) {
        return addressService.getAddressById(id);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<AddressResponse> addresses(@Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return addressService.getAllAddresses(PageRequest.of(p, s));
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<AddressResponse> addressesByUser(@Argument UUID userId, @Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return addressService.getAddressesByUserId(userId, PageRequest.of(p, s));
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<AddressResponse> shippingAddresses(@Argument UUID userId, @Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return addressService.getAddressesByUserIdAndType(userId, "shipping", PageRequest.of(p, s));
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<AddressResponse> billingAddresses(@Argument UUID userId, @Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return addressService.getAddressesByUserIdAndType(userId, "billing", PageRequest.of(p, s));
    }

    // ========================================================================
    // ADDRESS MUTATIONS
    // ========================================================================

    @MutationMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public AddressResponse createAddress(@Argument Map<String, Object> input) {
        CreateAddressRequest request = CreateAddressRequest.builder()
                .userId(UUID.fromString((String) input.get("userId")))
                .addressLine((String) input.get("addressLine"))
                .city((String) input.get("city"))
                .region((String) input.get("region"))
                .country((String) input.get("country"))
                .postalCode((String) input.get("postalCode"))
                .isDefault(input.get("isDefault") != null ? (Boolean) input.get("isDefault") : false)
                .addressType((String) input.get("addressType"))
                .build();
        return addressService.createAddress(request);
    }

    @MutationMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public AddressResponse updateAddress(@Argument UUID id, @Argument Map<String, Object> input) {
        CreateAddressRequest request = CreateAddressRequest.builder()
                .userId(UUID.fromString((String) input.get("userId")))
                .addressLine((String) input.get("addressLine"))
                .city((String) input.get("city"))
                .region((String) input.get("region"))
                .country((String) input.get("country"))
                .postalCode((String) input.get("postalCode"))
                .isDefault(input.get("isDefault") != null ? (Boolean) input.get("isDefault") : false)
                .addressType((String) input.get("addressType"))
                .build();
        return addressService.updateAddress(id, request);
    }

    @MutationMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public boolean deleteAddress(@Argument UUID id) {
        addressService.deleteAddress(id);
        return true;
    }


}

