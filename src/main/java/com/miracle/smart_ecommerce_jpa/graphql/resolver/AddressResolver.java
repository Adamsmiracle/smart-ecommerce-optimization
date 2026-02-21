package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateAddressRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.response.AddressResponse;
import com.miracle.smart_ecommerce_jpa.domain.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for Address entity.
 * Handles all address-related queries and mutations.
 */
@Controller
@RequiredArgsConstructor
public class AddressResolver {

    private final AddressService addressService;

    // ========================================================================
    // ADDRESS QUERIES
    // ========================================================================

    @QueryMapping
    public AddressResponse address(@Argument UUID id) {
        return addressService.getAddressById(id);
    }

    @QueryMapping
    public List<AddressResponse> addressesByUser(@Argument UUID userId) {
        return addressService.getAddressesByUserId(userId);
    }

    @QueryMapping
    public List<AddressResponse> shippingAddresses(@Argument UUID userId) {
        return addressService.getAddressesByUserIdAndType(userId, "shipping");
    }

    @QueryMapping
    public List<AddressResponse> billingAddresses(@Argument UUID userId) {
        return addressService.getAddressesByUserIdAndType(userId, "billing");
    }

    // ========================================================================
    // ADDRESS MUTATIONS
    // ========================================================================

    @MutationMapping
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
    public boolean deleteAddress(@Argument UUID id) {
        addressService.deleteAddress(id);
        return true;
    }


}

