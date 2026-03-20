package com.broadnet.billing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI Configuration for Broadnet Billing System
 *
 * Provides comprehensive API documentation for:
 * - Subscription management (plan changes, cancellations)
 * - Usage enforcement (real-time limit checking)
 * - Billing details (payment methods, addresses)
 * - Invoices (viewing, downloading)
 * - Dashboard (metrics, notifications, overview)
 * - Enterprise features (custom pricing, billing periods)
 * - Admin operations (plan limits, enterprise contacts)
 * - Checkout (Stripe integration)
 * - Usage analytics (reports, exports)
 * - Webhooks (Stripe event handling)
 *
 * Access Swagger UI at: /swagger-ui.html
 * Access OpenAPI JSON at: /v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Value("${swagger.server.url:http://localhost:8080}")
    private String serverUrl;

    @Value("${swagger.server.description:Development Server}")
    private String serverDescription;

    @Bean
    public OpenAPI billingSystemOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .components(securityComponents())
                .security(securityRequirements())
                .tags(apiTags());
    }

    /**
     * API metadata and documentation information
     */
    private Info apiInfo() {
        return new Info()
                .title("Broadnet Billing System API")
                .description("""
                        Comprehensive billing and subscription management system.
                        
                        ## Features
                        - **Subscription Management**: Plan changes, upgrades, downgrades, cancellations
                        - **Usage Enforcement**: Real-time usage tracking and limit enforcement
                        - **Billing Details**: Payment methods, billing addresses, Stripe portal access
                        - **Invoices**: View, download, and manage invoices
                        - **Dashboard**: Usage metrics, notifications, plan overview
                        - **Enterprise Billing**: Custom pricing, usage-based billing, contact management
                        - **Admin Tools**: Plan limit management, enterprise contact tracking
                        - **Analytics**: Usage reports, graphs, data exports
                        
                        ## Authentication
                        Most endpoints require authentication via JWT token in Authorization header.
                        Admin endpoints require additional ADMIN role.
                        
                        ## Rate Limiting
                        - Dashboard endpoints: 30 requests/minute
                        - Usage enforcement: Per company (not IP-based)
                        - Admin endpoints: 50 requests/minute
                        - Subscription changes: 5 requests/minute
                        - Analytics: 10 requests/minute
                        
                        ## Headers
                        - `Authorization`: Bearer JWT token (required for authenticated endpoints)
                        - `X-Company-Id`: Company identifier (required for most endpoints)
                        - `Stripe-Signature`: Webhook signature (webhooks only)
                        
                        ## Error Codes
                        - `400`: Bad Request - Invalid parameters
                        - `401`: Unauthorized - Missing or invalid authentication
                        - `403`: Forbidden - Insufficient permissions or limit exceeded
                        - `404`: Not Found - Resource doesn't exist
                        - `409`: Conflict - Version conflict or duplicate resource
                        - `500`: Internal Server Error - Server-side error
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Broadnet API Support")
                        .email("api-support@broadnet.com")
                        .url("https://broadnet.com/support"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://broadnet.com/license"));
    }

    /**
     * Server configuration
     */
    private List<Server> serverList() {
        return List.of(
                new Server()
                        .url(serverUrl)
                        .description(serverDescription),
                new Server()
                        .url("https://api.broadnet.com")
                        .description("Production Server"),
                new Server()
                        .url("https://staging-api.broadnet.com")
                        .description("Staging Server")
        );
    }

    /**
     * Security scheme definitions
     */
    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT authentication token. Obtain from /auth/login endpoint."))
                .addSecuritySchemes("companyHeader", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Company-Id")
                        .description("Company identifier extracted from authentication token or provided explicitly."))
                .addSecuritySchemes("stripeSignature", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("Stripe-Signature")
                        .description("Stripe webhook signature for event verification."));
    }

    /**
     * Global security requirements
     */
    private List<SecurityRequirement> securityRequirements() {
        return List.of(
                new SecurityRequirement().addList("bearerAuth"),
                new SecurityRequirement().addList("companyHeader")
        );
    }

    /**
     * API endpoint tags for organization
     */
    private List<Tag> apiTags() {
        return List.of(
                new Tag()
                        .name("Subscription Management")
                        .description("""
                                Manage subscription plans, upgrades, downgrades, and cancellations.
                                
                                **Rate Limit**: 5 requests/minute per user
                                
                                **Key Operations**:
                                - View available plans
                                - Change subscription plan
                                - Cancel subscription (immediate or end-of-period)
                                - Reactivate canceled subscription
                                """),

                new Tag()
                        .name("Usage Enforcement")
                        .description("""
                                Real-time usage tracking and limit enforcement.
                                
                                **Critical**: These endpoints block users if limits are exceeded.
                                **Performance**: Sub-100ms response time required.
                                **Rate Limit**: Per company, not IP-based.
                                
                                **Key Operations**:
                                - Increment answer usage (atomic)
                                - Check KB page limits
                                """),

                new Tag()
                        .name("Billing Details")
                        .description("""
                                Manage billing addresses and payment methods.
                                
                                **Rate Limit**: 10 requests/minute
                                
                                **Key Operations**:
                                - View billing details
                                - Update billing address
                                - Update payment method
                                - Access Stripe customer portal
                                """),

                new Tag()
                        .name("Invoices")
                        .description("""
                                View and download invoices.
                                
                                **Rate Limit**: 20 requests/minute
                                
                                **Key Operations**:
                                - List invoices (paginated)
                                - View invoice details
                                - Download invoice PDF
                                """),

                new Tag()
                        .name("Dashboard")
                        .description("""
                                Dashboard data aggregation and display.
                                
                                **Rate Limit**: 30 requests/minute
                                **Caching**: Aggressive caching with varying TTLs (30s-15min)
                                
                                **Key Operations**:
                                - View notifications
                                - Get current plan details
                                - View billing snapshot
                                - Check usage metrics
                                - List available boosts
                                - Get complete dashboard overview
                                - Purchase boost add-ons
                                """),

                new Tag()
                        .name("Enterprise Billing")
                        .description("""
                                Enterprise customer features and usage-based billing.
                                
                                **Rate Limit**: 20 requests/minute
                                
                                **Key Operations**:
                                - Submit enterprise contact request
                                - View enterprise summary (postpaid customers only)
                                - View billing periods and calculations
                                """),

                new Tag()
                        .name("Admin Operations")
                        .description("""
                                Administrative functions for plan and pricing management.
                                
                                **Authorization**: ADMIN role required
                                **Rate Limit**: 50 requests/minute
                                
                                **Key Operations**:
                                - Update plan limits (affects all companies)
                                - Set custom enterprise pricing
                                - Manage enterprise contact requests
                                - Track sales pipeline
                                """),

                new Tag()
                        .name("Checkout")
                        .description("""
                                Stripe checkout session creation for new subscriptions.
                                
                                **Rate Limit**: 10 requests/minute per user
                                
                                **Key Operations**:
                                - Create checkout session
                                """),

                new Tag()
                        .name("Usage Analytics")
                        .description("""
                                Usage reports, graphs, and data exports.
                                
                                **Rate Limit**: 10 requests/minute
                                **Caching**: 30min-1hr TTL for historical data
                                
                                **Key Operations**:
                                - Get usage summary with trends
                                - Get usage graphs for visualization
                                - Export usage data (CSV/JSON)
                                """),

                new Tag()
                        .name("Webhooks")
                        .description("""
                                Stripe webhook event handling.
                                
                                **Security**: Validates Stripe-Signature header
                                **No Rate Limit**: Webhook endpoint
                                
                                **Handled Events**:
                                - checkout.session.completed
                                - invoice.paid
                                - invoice.payment_failed
                                - customer.subscription.updated
                                - customer.subscription.deleted
                                """)
        );
    }
}