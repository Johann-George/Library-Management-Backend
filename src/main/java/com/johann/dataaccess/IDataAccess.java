package com.johann.dataaccess;

import com.johann.models.User;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.johann.models.Book;
import com.johann.models.BookCategory;
import com.johann.models.Order;


public interface IDataAccess {
    int createUser(User user);
    boolean isEmailAvailable(String email);
    User authenticateUser(String email, String password);
    List<Book> getAllBooks();
    boolean orderBook(int userId, int bookId);
    List<Order> getOrdersOfUser(int userId);
    List<Order> getAllOrders();
    boolean returnBook(int userId, int bookId);
    List<User> getUsers();
    void blockUser(int userId);
    void unblockUser(int userId);
    void deactivateUser(int userId);
    void activateUser(int userId);
    List<BookCategory> getAllCategories();
    void insertNewBook(Book book);
    boolean deleteBook(int bookId);
    void createCategory(BookCategory bookCategory);
//	int fetchfine(int userId);
}
