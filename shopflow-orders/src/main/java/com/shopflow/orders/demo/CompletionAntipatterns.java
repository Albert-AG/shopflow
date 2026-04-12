package com.shopflow.orders.demo;

import com.shopflow.orders.domain.model.Order;
import com.shopflow.orders.domain.model.OrderId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MATERIAL DIDÁCTICO — Tema 4: Completions Pro
 *
 * Este archivo contiene 5 antipatrones generados frecuentemente por IA.
 * El ejercicio del alumno: identifica los 5 problemas ANTES de leer los comentarios.
 *
 * NO usar este código en producción.
 * NO modificar este archivo.
 */
@Service
public class CompletionAntipatterns {

    // Antipatrón #3: HashMap en un @Service Singleton — no es thread-safe
    // El @Service es un singleton de Spring; HashMap puede corromperse bajo concurrencia.
    // Solución: usar ConcurrentHashMap o, mejor, externalizar el caché (Redis, Caffeine).
    private final Map<OrderId, Order> orderCache = new HashMap<>();

    private final FakeOrderRepository repository;

    public CompletionAntipatterns(FakeOrderRepository repository) {
        this.repository = repository;
    }

    /**
     * Antipatrón #1: Optional.get() sin verificación previa.
     * Si el pedido no existe, lanza NoSuchElementException sin contexto.
     * La IA genera este patrón cuando no tiene contexto sobre el manejo de errores.
     *
     * Solución: usar orElseThrow(() -> new OrderNotFoundException(id))
     */
    public Order findOrderUnsafe(OrderId id) {
        Optional<Order> order = repository.findById(id);
        return order.get(); // ← ANTIPATRÓN: puede lanzar NoSuchElementException
    }

    /**
     * Antipatrón #2: @Transactional en un método privado.
     * Spring AOP no intercepta métodos privados; la transacción nunca se crea.
     * La IA añade @Transactional sin verificar si el método es interceptable.
     *
     * Solución: mover la lógica transaccional a un método público,
     * o usar programmatic transactions.
     */
    @Transactional // ← ANTIPATRÓN: no tiene efecto en métodos privados
    private void updateOrderStatus(OrderId id, String newStatus) {
        // Esta transacción NUNCA se aplica. Si falla a mitad, la BD queda inconsistente.
        repository.updateStatus(id, newStatus);
        repository.logStatusChange(id, newStatus);
    }

    /**
     * Antipatrón #4: findAll() sin paginación.
     * Con miles de pedidos, esto carga toda la tabla en memoria y provoca OOM.
     * La IA genera findAll() por defecto; añadir Pageable requiere instrucción explícita.
     *
     * Solución: findAll(Pageable pageable) con Page<Order> como retorno.
     */
    public List<Order> getAllOrders() {
        return repository.findAll(); // ← ANTIPATRÓN: sin límite ni paginación
    }

    /**
     * Antipatrón #5: Parámetros invertidos en una query Between.
     * findByCreatedAtBetween(end, start) no lanzará error en compilación,
     * pero devolverá 0 resultados cuando end > start.
     * La IA invierte parámetros cuando las variables tienen nombres similares.
     *
     * Solución: nombrar las variables de forma inequívoca y revisar el orden
     * en la firma del método del repositorio.
     */
    public List<Order> findOrdersInRange(Instant start, Instant end) {
        return repository.findByCreatedAtBetween(end, start); // ← ANTIPATRÓN: invertidos
    }

    // ---- Interfaces de apoyo (solo para que el código compile) ----

    interface FakeOrderRepository {
        Optional<Order> findById(OrderId id);
        void updateStatus(OrderId id, String status);
        void logStatusChange(OrderId id, String status);
        List<Order> findAll();
        Page<Order> findAll(Pageable pageable);
        List<Order> findByCreatedAtBetween(Instant from, Instant to);
    }
}
