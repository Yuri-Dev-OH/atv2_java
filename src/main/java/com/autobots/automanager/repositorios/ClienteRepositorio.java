package com.autobots.automanager.repositorios;

import java.util.Optional;
import com.autobots.automanager.entidades.Cliente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> bloquearPorId(@Param("id") long id);
}
