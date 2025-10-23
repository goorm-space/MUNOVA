package com.space.munova.product.domain.Repository;

import com.space.munova.product.domain.Option;
import com.space.munova.product.domain.enums.OptionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OptionRepository extends JpaRepository<Option, Long> {
    boolean existsByOptionTypeAndOptionName(OptionCategory optionType, String optionName);

    Optional<Option> findByOptionTypeAndOptionName(OptionCategory optionType, String optionName);
}
