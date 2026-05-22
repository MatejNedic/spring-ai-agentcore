# Runtime Starter — Design Document

## Overview

The `spring-ai-agentcore-runtime-starter` module auto-configures a Spring Boot application to conform to the Amazon AgentCore Runtime contract. It discovers a user-defined method annotated with `@AgentCoreInvocation`, registers it directly as the Spring MVC handler for `POST /invocations`, and provides a `GET /ping` health endpoint. The user's method participates in the standard MVC pipeline — argument resolution, message conversion, validation, and content negotiation all work natively.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    AgentCoreAutoConfiguration                       │
│  (wires all beans, registers WebMvcConfigurer for arg resolvers)   │
└────────────┬──────────────────────┬─────────────────────────────────┘
             │                      │
             ▼                      ▼
┌────────────────────────┐  ┌──────────────────────────────────────┐
│  AgentCoreMethodScanner│  │  AgentCoreInvocationRegistrar        │
│  (BeanPostProcessor)   │  │  (SmartInitializingSingleton)        │
│                        │  │                                      │
│  • Scans all beans     │  │  • Reads InvocationTarget from       │
│  • Finds @AgentCore-   │  │    scanner                           │
│    Invocation method   │  │  • Checks for existing mapping       │
│  • Builds Invocation-  │  │  • Registers method into             │
│    Target (once)       │  │    RequestMappingHandlerMapping      │
└───────────┬────────────┘  └──────────────────────────────────────┘
            │
            ▼
┌────────────────────────┐
│    InvocationTarget    │
│    (record)            │
│  • bean                │
│  • method              │
│  • bodyParameter       │
│  • validationHints     │
└────────────────────────┘

── Request-time argument resolvers ──────────────────────────────────

┌─────────────────────────────────────┐  ┌──────────────────────────────────────────┐
│ AgentCoreContextArgumentResolver    │  │ AgentCoreInvocationBodyArgumentResolver   │
│                                     │  │                                          │
│ • Resolves AgentCoreContext param   │  │ • Resolves the body param (non-context)  │
│ • Extracts all HTTP headers         │  │ • Uses HttpMessageConverter to read      │
│                                     │  │ • Runs SmartValidator if @Valid present  │
└─────────────────────────────────────┘  └──────────────────────────────────────────┘
```

### Class Responsibilities

| Class | Role |
|-------|------|
| `AgentCoreMethodScanner` | `BeanPostProcessor` — discovers the single `@AgentCoreInvocation` method, resolves body parameter index and validation hints, stores as `InvocationTarget` |
| `InvocationTarget` | Immutable record holding bean, method, body `MethodParameter`, and precomputed validation hints |
| `AgentCoreInvocationRegistrar` | `SmartInitializingSingleton` — after all singletons are instantiated, registers the method into `RequestMappingHandlerMapping` as `POST /invocations`; skips if user already mapped that path |
| `AgentCoreContextArgumentResolver` | `HandlerMethodArgumentResolver` — populates `AgentCoreContext` from request headers |
| `AgentCoreInvocationBodyArgumentResolver` | `HandlerMethodArgumentResolver` — deserializes the request body via `HttpMessageConverter` (from a static default list: Jackson + String), validates with `SmartValidator` if hints are present |

## How It Works

### Startup

1. `AgentCoreAutoConfiguration` is activated by Spring Boot auto-configuration.
2. `AgentCoreMethodScanner` (a static bean — eligible for `BeanPostProcessor`) inspects every bean after initialization via `postProcessAfterInitialization`.
3. For each bean, `MethodIntrospector.selectMethods` finds methods annotated with `@AgentCoreInvocation`. If found, it builds an `InvocationTarget` record. A second annotated method throws `AgentCoreInvocationException`.
4. The `WebMvcConfigurer` bean registers `AgentCoreContextArgumentResolver` and `AgentCoreInvocationBodyArgumentResolver` into the MVC argument resolver chain.
5. After all singletons are instantiated, `AgentCoreInvocationRegistrar` (`SmartInitializingSingleton`):
   - Reads the `InvocationTarget` from the scanner.
   - Asserts a `Validator` bean exists if `@Valid`/`@Validated` is used.
   - Checks `RequestMappingHandlerMapping` for an existing `POST /invocations` mapping (user override).
   - If no override, calls `handlerMapping.registerMapping(...)` with the user's bean and method.

### First Request (`POST /invocations`)

1. `DispatcherServlet` routes to the registered handler method.
2. Spring MVC resolves arguments:
   - `AgentCoreContext` parameter → `AgentCoreContextArgumentResolver` extracts headers.
   - Body parameter → `AgentCoreInvocationBodyArgumentResolver` picks the matching `HttpMessageConverter`, reads the body in a single pass, validates if needed.
3. The user's method executes and returns (POJO, `String`, `Flux<String>`, `ResponseEntity`, etc.).
4. `@ResponseBody` (meta-annotated on `@AgentCoreInvocation`) triggers `HttpMessageConverter` serialization. For `Flux`, Spring's reactive SSE support handles streaming.

## Previous vs Current Implementation

| Aspect | Previous | Current |
|--------|----------|---------|
| **Endpoint registration** | `@RestController` with two `@PostMapping` methods (JSON + text) | User's method registered directly into `RequestMappingHandlerMapping` — no framework controller |
| **Body deserialization** | `AgentCoreMethodInvoker`: `Object` → Jackson `writeValueAsString` → Jackson `readValue(targetType)` (triple round-trip) | Single `HttpMessageConverter.read()` from `InputStream` — zero intermediate representations |
| **Method discovery** | `AgentCoreMethodRegistry`: stores single method, throws on duplicate | `AgentCoreMethodScanner` (`BeanPostProcessor`): builds `InvocationTarget` record with precomputed metadata |
| **AgentCoreContext** | Manually constructed in the controller and passed to invoker | `HandlerMethodArgumentResolver` — injected by Spring MVC like any other parameter |
| **Validation** | Not supported | `ValidationAnnotationUtils` + `SmartValidator` with group support; fail-fast at startup if validator missing |
| **User override** | Marker interface (`AgentCoreInvocationHandler`) checked via `@ConditionalOnMissingBean` | Mapping-level detection: if `POST /invocations` already registered, skip — no marker interface needed |
| **Content types** | Two separate handler methods (JSON, text/plain) | Single mapping with `consumes = {application/json, text/plain}`, `produces = {application/json, text/event-stream, application/octet-stream}` |
| **Return type flexibility** | Fixed to what the framework controller could serialize | Any Spring MVC–supported return type (`Flux`, `ResponseEntity`, POJO, `String`, `SseEmitter`, etc.) |

## Benefits

- **Zero-copy deserialization** — request body is read once directly into the target type; no intermediate `Object` or `String` representation.
- **Full Spring MVC integration** — the user's method is a first-class handler; exception handling, content negotiation, interceptors, and CORS all work out of the box.
- **Native validation** — `@Valid` / `@Validated` with group support; startup fails fast if the validator dependency is missing.
- **No marker interfaces** — override detection is automatic (existing mapping wins).
- **Precomputed metadata** — body parameter index and validation hints are resolved once at startup, not per request.
- **Flexible signatures** — users can declare any combination of `AgentCoreContext`, body POJO, `@RequestHeader`, `HttpServletRequest`, etc.
- **Streaming native** — returning `Flux<String>` produces SSE without any adapter layer.
- **Actuator-safe** — registrar looks up `requestMappingHandlerMapping` by name to avoid conflicts with Actuator's endpoint mappings.

## Usage Examples

```java
// Minimal — body only
@AgentCoreInvocation
public String handle(PromptRequest request) { ... }

// With context (headers)
@AgentCoreInvocation
public String handle(PromptRequest request, AgentCoreContext context) { ... }

// Streaming SSE
@AgentCoreInvocation
public Flux<String> stream(PromptRequest request, AgentCoreContext context) { ... }

// With validation
@AgentCoreInvocation
public ResponseEntity<Answer> handle(@Valid PromptRequest request, AgentCoreContext context) { ... }

// Full Spring MVC parameter injection
@AgentCoreInvocation
public ResponseEntity<Answer> handle(
        @Valid PromptRequest request,
        AgentCoreContext context,
        @RequestHeader("Authorization") String auth,
        HttpServletRequest raw) { ... }

// Context only (no body)
@AgentCoreInvocation
public String handle(AgentCoreContext context) { ... }

// Raw string body
@AgentCoreInvocation
public String handle(String body) { ... }
```

### Parameter Resolution Rules

The user's method is a real Spring MVC handler. Parameters are resolved in this order:

1. **Built-in annotation resolvers** — `@RequestHeader`, `@PathVariable`, `@RequestParam`, `@CookieValue`, `@RequestAttribute`, etc. These always run first.
2. **Built-in type resolvers** — `HttpServletRequest`, `HttpServletResponse`, `Principal`, `Locale`, `InputStream`, etc.
3. **AgentCore custom resolvers** (added via `WebMvcConfigurer`):
   - `AgentCoreContextArgumentResolver` — claims `AgentCoreContext` parameters.
   - `AgentCoreInvocationBodyArgumentResolver` — claims the **first** non-context, non-Spring-annotated parameter as the request body.
4. **Catch-all** — `ModelAttributeMethodProcessor` (form binding).

**Rule: one body parameter.** The first unannotated, non-`AgentCoreContext` parameter is treated as the JSON/text request body. All other parameters must be resolvable by Spring's built-in resolvers (i.e. annotated with `@RequestHeader`, `@CookieValue`, etc., or be a recognized type like `HttpServletRequest`).

## Known Limitations

- **`Map` body parameter** — `Map<String, Object>` is not auto-detected as a body type by the custom resolver because `HttpMessageConverter.canRead` may not match without an explicit `@RequestBody`. Workaround: annotate with `@RequestBody` and use a standard `@PostMapping` controller instead.
- **Single method per application** — only one `@AgentCoreInvocation` method is allowed. Multiple endpoints require a traditional `@RestController`.
- **Servlet only** — this starter targets Spring MVC (Servlet stack). WebFlux (Netty) is not supported. See *Future: WebFlux Starter* below.
- **Validation requires starter** — `@Valid` on the body parameter requires `spring-boot-starter-validation` on the classpath; its absence causes a startup failure (by design).
- **No multipart support** — the body resolver reads the full input stream as a single object; multipart form data is not handled.

## Future: WebFlux Starter

The current module (`spring-ai-agentcore-runtime-starter`) is Servlet/Spring MVC only. To support reactive applications (WebFlux/Netty), a **separate starter** should be created:

| | MVC Starter (current) | WebFlux Starter (future) |
|---|---|---|
| Module | `spring-ai-agentcore-runtime-starter` | `spring-ai-agentcore-runtime-starter-webflux` |
| Conditional | `@ConditionalOnWebApplication(type = SERVLET)` | `@ConditionalOnWebApplication(type = REACTIVE)` |
| Configurer | `WebMvcConfigurer` | `WebFluxConfigurer` |
| Resolver interface | `o.s.web.method.support.HandlerMethodArgumentResolver` | `o.s.web.reactive.result.method.HandlerMethodArgumentResolver` |
| Request access | `HttpServletRequest` | `ServerWebExchange` / `ServerHttpRequest` |
| Body reading | `HttpMessageConverter.read(Class, HttpInputMessage)` | `HttpMessageReader.read(ResolvableType, ReactiveHttpInputMessage)` → `Mono<T>` |
| Handler mapping | `o.s.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping` | `o.s.web.reactive.result.method.annotation.RequestMappingHandlerMapping` |
| Registration | `handlerMapping.registerMapping(...)` | Same API (reactive variant) |

### Shared between both starters

- `@AgentCoreInvocation` annotation (in a shared `spring-ai-agentcore-runtime-common` or the existing `common` module)
- `AgentCoreContext` / `AgentCoreHeaders`
- `InvocationTarget` record
- `AgentCoreMethodScanner` (BPP — framework-agnostic, only uses reflection)
- Ping endpoint model (`AgentCorePingResponse`, `PingStatus`)

### WebFlux-specific classes needed

- `AgentCoreReactiveContextArgumentResolver` — extracts headers from `ServerHttpRequest`
- `AgentCoreReactiveBodyArgumentResolver` — reads body via `HttpMessageReader` (Jackson decoder)
- `AgentCoreReactiveInvocationRegistrar` — registers into the reactive `RequestMappingHandlerMapping`
- `AgentCoreWebFluxAutoConfiguration` — wires the above via `WebFluxConfigurer`

Pattern reference: Spring Cloud AWS SNS uses this exact split (`WebMvcConfigurer` vs `WebFluxConfigurer` in separate `@Configuration` classes gated by `@ConditionalOnWebApplication`).

## Critical Assessment

### Pros

1. **User's method is the handler.** No framework controller in between. Spring MVC's full infrastructure applies: `@ResponseBody`, `@ExceptionHandler`, `HandlerInterceptor`, content negotiation, CORS, async support (`Flux`, `DeferredResult`).
2. **Single Jackson read.** One `HttpMessageConverter.read()` from the raw `InputStream` — no intermediate representations.
3. **Precomputed metadata.** Body parameter index, validation hints, annotation checks — all resolved once at startup. No reflection at request time.
4. **Clean separation of concerns.** Scanner finds the method. Registrar wires it. Resolvers handle arguments. Each class does one thing.
5. **Fail-fast on misconfiguration.** Duplicate methods, missing validator — caught at startup with clear messages.
6. **Override detection without marker interfaces.** Checks if `POST /invocations` is already mapped. Works with any user controller style.
7. **Proven patterns.** `SmartInitializingSingleton`, static converter lists, `WebMvcConfigurer` anonymous class — same patterns used in Spring Cloud AWS.

### Cons

1. **More code.** ~350 lines vs ~260 in the old implementation. More classes (5 service + 1 record vs 4 in the old design).
2. **`RequestMappingHandlerMapping` name-based lookup.** Required because Actuator introduces a second `RequestMappingHandlerMapping` bean. Fragile if Spring ever renames the bean (unlikely but not impossible).
3. **Static converter list.** `DEFAULT_MESSAGE_CONVERTERS` doesn't pick up user-customized `ObjectMapper` or additional converters registered via `WebMvcConfigurer.extendMessageConverters`. Users who need custom serialization must use `@RequestBody` (which goes through Spring's converter chain).
4. **Annotation detection list is manual.** `hasSpringMvcAnnotation` enumerates 8 annotations explicitly. A new Spring MVC binding annotation in a future version would need to be added manually.
5. **Single method per app.** Architectural constraint — users wanting multiple endpoints must fall back to a regular `@RestController`. No multi-method routing.
6. **Servlet-only.** WebFlux requires a separate starter with mirrored classes (different resolver interface, different handler mapping, different body reading API).
7. **`Map` parameters need `@RequestBody`.** Spring's built-in `MapMethodProcessor` claims unannotated `Map` params first. Not a bug we introduced, but a UX friction point users will hit.

### Remaining rough edges

| # | Issue | Severity |
|---|---|---|
| 1 | `hasSpringMvcAnnotation` logic duplicated between scanner and body resolver | Low — extract shared utility |
| 2 | `DEFAULT_MESSAGE_CONVERTERS` exposes mutable converter instances (e.g. `ObjectMapper` on Jackson converter is configurable) | Low — nobody reconfigures static converters in practice |
| 3 | `@Nullable Validator` field in body resolver could NPE if resolver is constructed manually without validator while target has hints | Low — not a supported construction path; startup fail-fast prevents this in normal usage |
| 4 | No `@ConditionalOnWebApplication(type = SERVLET)` on autoconfig | Low — functionally fine (registrar skips when no `requestMappingHandlerMapping`), but imprecise conditional |
| 5 | Unused imports in `AgentCoreAutoConfiguration` (`HttpMessageConverters`, etc.) | Cosmetic |

### Metrics comparison

| Metric | Old | New |
|---|---|---|
| Jackson ops/request | 3 | 1 |
| Reflection/request | `method.getParameterTypes()` every time | 0 |
| Lines of framework code | ~260 (controller + invoker + registry + scanner) | ~350 (scanner + registrar + 2 resolvers + target + autoconfig) |
| Features | Single method, no validation, no override detection, no flexible params | Single method, `@Valid`, override detection, full Spring MVC params |
| Spring MVC integration | Partial (own controller, own deserialization) | Full (user method IS the handler) |
| Patterns | Custom, ad-hoc | Matches Spring Cloud AWS SNS, Spring Boot conventions |

The new code is ~90 lines longer but does significantly more, and the patterns are recognizable to anyone who's worked with Spring Boot starters or Spring Cloud.
