package io.github.johaosalazarcuyos.shopflow;

import org.springframework.boot.SpringApplication;

public class TestShopflowApplication {

	public static void main(String[] args) {
		SpringApplication.from(ShopflowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
