package com.invoice.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.invoice.api.dto.ApiResponse;
import com.invoice.api.dto.DtoCustomer;
import com.invoice.api.dto.DtoProduct;
import com.invoice.api.entity.Cart;
import com.invoice.api.repository.RepoCart;
import com.invoice.configuration.client.CustomerClient;
import com.invoice.configuration.client.ProductClient;
import com.invoice.exception.ApiException;

@Service
public class SvcCartImp implements SvcCart {

	@Autowired
	RepoCart repo;
	
	@Autowired
	CustomerClient customerCl;
	
	@Autowired
	ProductClient productCl;
	
	@Override
	public List<Cart> getCart(String rfc) {
		return repo.findByRfcAndStatus(rfc,1);
	}

	@Override
	public ApiResponse addToCart(Cart cart) {
		if(!validateCustomer(cart.getRfc()))
			throw new ApiException(HttpStatus.BAD_REQUEST, "customer does not exist");
			
		/*
		 * Sprint 1 - Requerimiento 2
		 * Validar que el GTIN exista. Si existe, asignar el stock del producto a la variable product_stock 
		 */
		DtoProduct producto = getProduct(cart.getGtin());
		if (producto == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "product does not exist");
		}
		
		Integer product_stock = producto.getStock(); 
		if(cart.getQuantity() > product_stock || cart.getQuantity() <= 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "invalid quantity");
		}
		
		/*
		 * Sprint 2 - Requerimiento 3
		 * Validar si el producto ya había sido agregado al carrito para solo actualizar su cantidad
		 */
		/*
		List<Cart> carrito = getCart(cart.getRfc());
		for (Cart articulo : carrito) {
			if (articulo.getGtin().equals(cart.getGtin())) {
				Integer nueva_cantidad = articulo.getQuantity() + cart.getQuantity();
				if (nueva_cantidad > product_stock) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "invalid quantity");
				}
				
				repo.updateProductQuantity(articulo.getCart_id(), nueva_cantidad);
				return new ApiResponse("quantity updated");
			}
		}
		*/
		
		Cart articulo = repo.findByRfcAndGtinAndStatus(cart.getRfc(), cart.getGtin(), 1);
		if (articulo != null) {
			Integer nueva_cantidad = articulo.getQuantity() + cart.getQuantity();
			if (nueva_cantidad > product_stock) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "invalid quantity");
			}
			
			repo.updateProductQuantity(articulo.getCart_id(), nueva_cantidad);
			return new ApiResponse("quantity updated");
		}
		
		cart.setStatus(1);
		repo.save(cart);
		return new ApiResponse("item added");
	}

	@Override
	public ApiResponse removeFromCart(Integer cart_id) {
		if (repo.removeFromCart(cart_id) > 0)
			return new ApiResponse("item removed");
		else
			throw new ApiException(HttpStatus.BAD_REQUEST, "item cannot be removed");
	}

	@Override
	public ApiResponse clearCart(String rfc) {
		if (repo.clearCart(rfc) > 0)
			return new ApiResponse("cart removed");
		else
			throw new ApiException(HttpStatus.BAD_REQUEST, "cart cannot be removed");
	}
	
	private boolean validateCustomer(String rfc) {
		try {
			ResponseEntity<DtoCustomer> response = customerCl.getCustomer(rfc);
			if(response.getStatusCode() == HttpStatus.OK)
				return true;
			else
				return false;
		}catch(Exception e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "unable to retrieve customer information");
		}
	}
	
	private DtoProduct getProduct(String gtin) {
		try {
			ResponseEntity<DtoProduct> response = productCl.getProduct(gtin);
			if (response.getStatusCode() == HttpStatus.OK) {
				return response.getBody();
			}
			
			return null;
		} catch (Exception e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "unable to retrieve product information");
		}
	}

}
