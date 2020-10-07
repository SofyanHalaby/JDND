package com.example.demo;

import com.example.demo.controllers.CartController;
import com.example.demo.controllers.ItemController;
import com.example.demo.controllers.OrderController;
import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RunWith(SpringRunner.class)
//@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureMockMvc ////https://stackoverflow.com/questions/45241566/spring-boot-unit-tests-with-jwt-token-security
public class SareetaApplicationTests {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	ItemController itemController;
	@Autowired
	OrderController orderController;
	@Autowired
	UserController userController;
	@Autowired
	CartController cartController;
	@Autowired
	UserRepository userRepository;
	@Autowired
	private CartRepository cartRepository;
	@Autowired
	private ItemRepository itemRepository;
	@Autowired
	OrderRepository orderRepository;
	//Spies
	private UserRepository spyUserRepository;
	private OrderRepository spyOrderRepository;


	@Before
	public void setup() throws NoSuchFieldException, IllegalAccessException {
		spyUserRepository = Mockito.spy(UserRepository.class);
		spyOrderRepository = Mockito.spy(OrderRepository.class);
		Mockito.when(spyUserRepository.findByUsername("test")).thenReturn(buildUser("test"));
		Mockito.when(spyOrderRepository.save(Mockito.any(UserOrder.class))).thenReturn(new UserOrder());
		Mockito.when(spyOrderRepository.findByUser(Mockito.any(User.class))).thenReturn(Collections.singletonList(new UserOrder()));
		TestUtils.injectObject(cartController, "userRepository", spyUserRepository);
		TestUtils.injectObject(orderController,"userRepository", spyUserRepository);
		TestUtils.injectObject(orderController,"orderRepository", spyOrderRepository);
//		cartController = new CartController(spyUserRepository,cartRepository,itemRepository);
//		orderController = new OrderController(spyUserRepository,spyOrderRepository);
	}

	@After
	public void cleanup(){
		spyOrderRepository.deleteAll();
		orderRepository.deleteAll();
		cartRepository.deleteAll();
		spyUserRepository.deleteAll();
		userRepository.deleteAll();
//		itemRepository.deleteAll();
	}


	@Test
	public void contextLoads() {
	}

	//Item Controller

	@Test
	public void testGetItems(){
		List<Item> items = itemController.getItems().getBody();
		assert Objects.requireNonNull(items).size() == 2;
	}

	@Test
	public void testGetItemById(){
		Item item = itemController.getItemById(1L).getBody();
		assert Objects.requireNonNull(item).getName().equals("Round Widget");
	}

	@Test
	public void testGetItemByName(){
		List<Item> items = itemController.getItemsByName("Round Widget").getBody();

		assert  Objects.requireNonNull(items).size() == 1;
	}

	//Cart Controller
	@Test
	public void testAddToCart(){
		ModifyCartRequest cartRequest = buildModifyCartRequest(1, 3);
		Cart cart = cartController.addTocart(cartRequest).getBody();
		assert cart != null;
		assert cart.getItems() != null;
		assert cart.getItems().size() == cartRequest.getQuantity();
		assert cart.getItems().get(0).getId() == cartRequest.getItemId();
	}

	@Test
	public void removeFromCart(){
		ModifyCartRequest cartRequest = buildModifyCartRequest(1, 3);
		cartController.addTocart(cartRequest).getBody();
		cartRequest.setQuantity(2);
		Cart cart = cartController.removeFromcart(cartRequest).getBody();
		assert cart != null;
		assert cart.getItems() != null;
		assert cart.getItems().size() == 1;
	}


	//OrderController
	@Test
	public void testSubmitOrder(){
		ModifyCartRequest cartRequest = buildModifyCartRequest(1,3);
		cartController.addTocart(cartRequest).getBody();
		UserOrder order = orderController.submit(cartRequest.getUsername()).getBody();
		assert Objects.requireNonNull(order).getItems().size() == cartRequest.getQuantity();
	}

	@Test
	public void testSubmitForNullUser(){
		ResponseEntity<UserOrder> response = orderController.submit("noUser");
		assert response.getStatusCode() == HttpStatus.NOT_FOUND;
		assert response.getBody() == null;
	}

	@Test
	public void testGetOrdersForUser(){
		ModifyCartRequest cartRequest = buildModifyCartRequest(1,3);
		cartController.addTocart(cartRequest).getBody();
		orderController.submit(cartRequest.getUsername()).getBody();
		List<UserOrder> userOrders =
				orderController.getOrdersForUser(cartRequest.getUsername()).getBody();
		assert Objects.requireNonNull(userOrders).size() == 1;
	}

	@Test
	public void testGetOrdersForNullUser(){
		ResponseEntity<List<UserOrder>> response =
				orderController.getOrdersForUser("noUser");
		assert response.getStatusCode() == HttpStatus.NOT_FOUND;
	}

	//User Controller
	@Test
	public void testCreateUser(){
		CreateUserRequest createUserRequest =
				buildCreateUserRequest("asd", "myPassword", "myPassword");
		User user = userController.createUser(createUserRequest).getBody();
		assert user != null;
		assert user.getUsername().equals(createUserRequest.getUsername());
		assert !user.getPassword().equals(createUserRequest.getPassword());
	}

	@Test
	public void testCreateUserWithDifferentConfirmPassword(){
		CreateUserRequest createUserRequest =
				buildCreateUserRequest("asd", "myPassword", "differentPass");
		ResponseEntity<User> response = userController.createUser(createUserRequest);
		assert response.getBody() == null;
		assert response.getStatusCode() == HttpStatus.BAD_REQUEST;
	}

	@Test
	public void testFindUserById(){
		CreateUserRequest createUserRequest =
				buildCreateUserRequest("asd", "myPassword", "myPassword");
		User savedUser = userController.createUser(createUserRequest).getBody();
		assert savedUser != null;
		User foundUser = userController.findById(savedUser.getId()).getBody();
		assert foundUser != null;
		assert foundUser.getUsername().equals(createUserRequest.getUsername());
	}

	@Test
	public void testFindUserByName(){
		CreateUserRequest createUserRequest =
				buildCreateUserRequest("asd", "myPassword", "myPassword");
		userController.createUser(createUserRequest).getBody();
		User user = userController.findByUserName(createUserRequest.getUsername()).getBody();
		assert user != null;
	}

	//Authentication & Authorization
	//https://stackoverflow.com/questions/45241566/spring-boot-unit-tests-with-jwt-token-security
	@Test
	public void testUnauthorizedAccess() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/item"))
				.andExpect(MockMvcResultMatchers.status().isForbidden());
	}

	@Test
	public void testAuthorizedAccess() throws Exception {
		userController.createUser(buildCreateUserRequest("sofyan","12345@Pass","12345@Pass"));
		String loginRequestJsonBody = "{\"username\":\"sofyan\",\"password\":\"12345@Pass\"}";

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/login")
				.content(loginRequestJsonBody))
				.andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
		String response = result.getResponse().getHeader("Authorization");

		response = response.replace("{\"access_token\": \"", "");
		String token = response.replace("\"}", "");

		mockMvc.perform(MockMvcRequestBuilders.get("/api/item")
				.header("Authorization", "Bearer " + token))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	//Utils
	private static ModifyCartRequest buildModifyCartRequest(long itemId, int quantity){
		ModifyCartRequest modifyCartRequest = new ModifyCartRequest();
		modifyCartRequest.setUsername("test");
		modifyCartRequest.setItemId(itemId);
		modifyCartRequest.setQuantity(quantity);
		return modifyCartRequest;
	}

	private static CreateUserRequest buildCreateUserRequest(String username, String password, String confirmPassword){
		CreateUserRequest createUserRequest = new CreateUserRequest();
		createUserRequest.setUsername(username);
		createUserRequest.setPassword(password);
		createUserRequest.setConfirmPassword(confirmPassword);
		return createUserRequest;
	}

	private User buildUser(String username){
		User user = new User();
		user.setUsername(username);

		user.setPassword("password");

		Cart cart = new Cart();
		cart.setItems(null);
		user.setCart(new Cart());

		return user;
	}






}
