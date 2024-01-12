package com.johann.rest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.johann.dataaccess.IDataAccess;
import com.johann.dataaccess.Jwt;
import com.johann.models.Book;
import com.johann.models.BookCategory;
import com.johann.models.Order;
import com.johann.models.User;
import com.johann.models.UserType;

@RestController
@RequestMapping("/api/library")
@CrossOrigin
public class LibraryController {
	
	private final IDataAccess library;
    private final Jwt jwt;

    //@Autowired
    public LibraryController(IDataAccess library,Jwt jwt) {
        this.library = library;
        this.jwt = jwt;
    }

    @PostMapping("/CreateAccount")
    public String createAccount(@RequestBody User user) {
        if (!library.isEmailAvailable(user.getEmail())) {
            return "Email is not available!";
        }
        user.setCreatedOn(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        user.setUserType(UserType.USER);
        library.createUser(user);
        return "Account created successfully!";
    }

    //@GetMapping("/Login/{email}/{password}")
    //http://localhost:9091/api/library/Login?email=johann@gmail.com&password=joh@12345
    @GetMapping("/Login")
    public String login(@RequestParam String email, @RequestParam String password) {
    	
    	User user= library.authenticateUser(email, password);
    	System.out.println(user.toString());
    	
            if (user != null) {
                String token = jwt.generateToken(user);
                System.out.println(token);
                return token;
            }
      
        return "Invalid";
    }

    @GetMapping("/GetAllBooks")
    public List<Object> getAllBooks() {
        List<Book> books = library.getAllBooks();
        return books.stream()
                .map(book -> new HashMap<String, Object>() {{
                    put("id", book.getId());
                    put("title", book.getTitle());
                    put("category", book.getCategory().getCategory());
                    put("subCategory", book.getCategory().getSubCategory());
                    put("price", book.getPrice());
                    put("available", !book.isOrdered());
                    put("author", book.getAuthor());
                }})
                .collect(Collectors.toList());
    }

    @GetMapping("/OrderBook/{userId}/{bookId}")
    public String orderBook(@PathVariable int userId, @PathVariable int bookId) {
        return library.orderBook(userId, bookId) ? "success" : "fail";
    }

    @GetMapping("/GetOrders/{id}")
    public List<Order> getOrders(@PathVariable int id) {
        return library.getOrdersOfUser(id);
    }

    @GetMapping("/GetAllOrders")
    public List<Order> getAllOrders() {
        return library.getAllOrders();
    }

    @GetMapping("/ReturnBook/{bookId}/{userId}")
    public String returnBook(@PathVariable String bookId, @PathVariable String userId) {
        return library.returnBook(Integer.parseInt(userId), Integer.parseInt(bookId)) ? "success" : "not returned";
    }

    @GetMapping("/GetAllUsers")
    public List<Object> getAllUsers() {
        List<User> users = library.getUsers();
        return users.stream()
                .map(user -> new HashMap<String, Object>() {{
                    put("id", user.getId());
                    put("firstName", user.getFirstName());
                    put("lastName", user.getLastName());
                    put("email", user.getEmail());
                    put("mobile", user.getMobile());
                    put("blocked", user.isBlocked());
                    put("active", user.isActive());
                    put("createdOn", user.getCreatedOn());
                    put("userType", user.getUserType());
                    put("fine", user.getFine());
                }})
                .collect(Collectors.toList());
    }

    @GetMapping("/ChangeBlockStatus/{status}/{id}")
    public String changeBlockStatus(@PathVariable int status, @PathVariable int id) {
        if (status == 1) {
            library.blockUser(id);
        } else {
            library.unblockUser(id);
        }
        return "success";
    }

    @GetMapping("/ChangeEnableStatus/{status}/{id}")
    public String changeEnableStatus(@PathVariable int status, @PathVariable int id) {
        if (status == 1) {
            library.activateUser(id);
        } else {
            library.deactivateUser(id);
        }
        return "success";
    }

    @GetMapping("/GetAllCategories")
    public List<Object> getAllCategories() {
        List<BookCategory> categories = library.getAllCategories();
        return categories.stream()
                .collect(Collectors.groupingBy(BookCategory::getCategory))
                .entrySet().stream()
                .map(entry -> new HashMap<String, Object>() {{
                    put("name", entry.getKey());
                    put("children", entry.getValue().stream()
                            .map(subCategory -> new HashMap<String, Object>() {{
                                put("name", subCategory.getSubCategory());
                            }})
                            .collect(Collectors.toList()));
                }})
                .collect(Collectors.toList());
    }

    @PostMapping("/InsertBook")
    public String insertBook(@RequestBody Book book) {
        book.setTitle(book.getTitle().trim());
        book.setAuthor(book.getAuthor().trim());
        book.getCategory().setCategory(book.getCategory().getCategory().toLowerCase());
        book.getCategory().setSubCategory(book.getCategory().getSubCategory().toLowerCase());

        library.insertNewBook(book);
        return "Inserted";
    }

    @DeleteMapping("/DeleteBook/{id}")
    public String deleteBook(@PathVariable int id) {
        return library.deleteBook(id) ? "success" : "fail";
    }

    @PostMapping("/InsertCategory")
    public String insertCategory(@RequestBody BookCategory bookCategory) {
        bookCategory.setCategory(bookCategory.getCategory().toLowerCase());
        bookCategory.setSubCategory(bookCategory.getSubCategory().toLowerCase());
        library.createCategory(bookCategory);
        return "Inserted";
    }

}
