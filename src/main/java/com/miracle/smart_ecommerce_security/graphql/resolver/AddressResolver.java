package com.miracle.smart_ecommerce_security.graphql.resolver;

import com.miracle.smart_ecommerce_security.domain.user.dto.request.CreateAddressRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.response.AddressResponse;
import com.miracle.smart_ecommerce_security.domain.user.service.AddressService;
import com.miracle.smart_ecommerce_security.graphql.type.GraphQLPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for Address entity.
 * Handles all address-related queries and mutations.
 *
 * Access: ADMIN and CUSTOMER roles.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public GraphQLPage<AddressResponse> addresses(@Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return GraphQLPage.of(addressService.getAllAddresses(PageRequest.of(p, s)));
    }

    @QueryMapping
    public GraphQLPage<AddressResponse> addressesByUser(@Argument UUID userId, @Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return GraphQLPage.of(addressService.getAddressesByUserId(userId, PageRequest.of(p, s)));
    }

    @QueryMapping
    public GraphQLPage<AddressResponse> shippingAddresses(@Argument UUID userId, @Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return GraphQLPage.of(addressService.getAddressesByUserIdAndType(userId, "shipping", PageRequest.of(p, s)));
    }

    @QueryMapping
    public GraphQLPage<AddressResponse> billingAddresses(@Argument UUID userId, @Argument Integer page, @Argument Integer size) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;
        return GraphQLPage.of(addressService.getAddressesByUserIdAndType(userId, "billing", PageRequest.of(p, s)));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public boolean deleteAddress(@Argument UUID id) {
        addressService.deleteAddress(id);
        return true;
    }
}
