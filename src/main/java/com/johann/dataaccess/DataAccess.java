package com.johann.dataaccess;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.johann.models.Book;
import com.johann.models.BookCategory;
import com.johann.models.Order;
import com.johann.models.User;

@Service
public class DataAccess implements IDataAccess {

	private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataAccess(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
	@Override
	public int createUser(User user) {
		int result = 0;

        String sql = "INSERT INTO Users (FirstName, LastName, Email, Mobile, Password, Blocked, Active, CreatedOn, UserType) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            result = jdbcTemplate.update(sql,
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getMobile(),
                    user.getPassword(),
                    user.isBlocked(),
                    user.isActive(),
                    user.getCreatedOn(),
                    user.getUserType().toString());
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
        }

        return result;
	}

	@Override
	public boolean isEmailAvailable(String email) {
		String sql = "SELECT COUNT(*) FROM Users WHERE Email = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, email);

        return count == 0;
	}

	@Override
	public User authenticateUser(String email, String password) {
		String countSql = "SELECT COUNT(1) FROM Users WHERE email = ? AND password = ?";
        int count = jdbcTemplate.queryForObject(countSql, Integer.class, email, password);

        User user=null;
        if (count > 0) {
            String selectSql = "SELECT * FROM Users WHERE email = ?";
            user = jdbcTemplate.queryForObject(selectSql, new BeanPropertyRowMapper<>(User.class), email);
            return user;
        } else {
            user = null;
            return null;
        }
	}

	@Override
	public List<Book> getAllBooks() {
		String sql = "SELECT * FROM Books";
        List<Book> books = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Book.class));

        for (Book book : books) {
            sql = "SELECT * FROM BookCategories WHERE Id = ?";
            BookCategory category = jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(BookCategory.class), book.getCategoryId());
            book.setCategory(category);
        }

        return books;
	}

	@Override
	public boolean orderBook(int userId, int bookId) {
		boolean ordered = false;

        String orderSql = "INSERT INTO Orders (UserId, BookId, OrderedOn, Returned) VALUES (?, ?, ?, ?)";
        String updateBookSql = "UPDATE Books SET Ordered = 1 WHERE Id = ?";

        try {
            int insertedRows = jdbcTemplate.update(orderSql, userId, bookId, new Date(), 0);

            if (insertedRows == 1) {
                int updatedRows = jdbcTemplate.update(updateBookSql, bookId);
                ordered = updatedRows == 1;
            }
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
        }

        return ordered;
	}

	@Override
	public List<Order> getOrdersOfUser(int userId) {
		String sql = "SELECT " +
                "o.Id, " +
                "u.Id AS UserId, CONCAT(u.FirstName, ' ', u.LastName) AS Name, " +
                "o.BookId AS BookId, b.Title AS BookName, " +
                "o.OrderedOn AS OrderDate, o.Returned AS Returned " +
                "FROM Users u " +
                "LEFT JOIN Orders o ON u.Id = o.UserId " +
                "LEFT JOIN Books b ON o.BookId = b.Id " +
                "WHERE o.UserId IN (?)";

		return jdbcTemplate.query(sql, new Object[]{userId}, new BeanPropertyRowMapper<>(Order.class));
	}

	@Override
	public List<Order> getAllOrders() {
		String sql = "SELECT " +
                "o.Id, " +
                "u.Id AS UserId, CONCAT(u.FirstName, ' ', u.LastName) AS Name, " +
                "o.BookId AS BookId, b.Title AS BookName, " +
                "o.OrderedOn AS OrderDate, o.Returned AS Returned " +
                "FROM Users u " +
                "LEFT JOIN Orders o ON u.Id = o.UserId " +
                "LEFT JOIN Books b ON o.BookId = b.Id " +
                "WHERE o.Id IS NOT NULL";

		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Order.class));
	}

	@Override
	public boolean returnBook(int userId, int bookId) {
		String updateBooksSql = "UPDATE Books SET Ordered = 0 WHERE Id = ?";
        String updateOrdersSql = "UPDATE Orders SET Returned = 1 WHERE UserId = ? AND BookId = ?";
        
        try {
            jdbcTemplate.update(updateBooksSql, bookId);
            int updatedRows = jdbcTemplate.update(updateOrdersSql, userId, bookId);
            return updatedRows == 1;
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
            return false;
        }
	}

	@Override
	public List<User> getUsers() {
		String usersSql = "SELECT * FROM Users";
        List<User> users = jdbcTemplate.query(usersSql, new BeanPropertyRowMapper<>(User.class));

        String ordersSql = "SELECT u.Id AS UserId, o.BookId AS BookId, o.OrderedOn AS OrderDate, o.Returned AS Returned " +
                           "FROM Users u " +
                           "LEFT JOIN Orders o ON u.Id = o.UserId";
        List<OrderInfo> orderInfoList = jdbcTemplate.query(ordersSql, new BeanPropertyRowMapper<>(OrderInfo.class));

        for (User user : users) {
            List<OrderInfo> userOrders = getUserOrders(orderInfoList, user.getId());
            int fine = calculateFine(userOrders);
            user.setFine(fine);
        }

        return users;
	}
	
	//private
	private List<OrderInfo> getUserOrders(List<OrderInfo> orderInfoList, int userId) {
        return orderInfoList.stream()
                .filter(orderInfo -> orderInfo.getUserId() == userId)
                .collect(Collectors.toList());
    }

    private int calculateFine(List<OrderInfo> orders) {
        int fine = 0;

        for (OrderInfo order : orders) {
            if (order.getBookId() != null && order.getReturned() != null && !order.getReturned()) {
                long extraDays = Math.max(0, (System.currentTimeMillis() - order.getOrderDate().getTime()) / (24 * 60 * 60 * 1000) - 10);
                fine += extraDays * 50;
            }
        }

        return fine;
    }
	
    private static class OrderInfo {
        private int userId;
        private Integer bookId;
        private java.sql.Date orderDate;
        private Boolean returned;

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public Integer getBookId() {
            return bookId;
        }

        public void setBookId(Integer bookId) {
            this.bookId = bookId;
        }

        public java.sql.Date getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(java.sql.Date orderDate) {
            this.orderDate = orderDate;
        }

        public Boolean getReturned() {
            return returned;
        }

        public void setReturned(Boolean returned) {
            this.returned = returned;
        }
    }

    	
	//

	@Override
	public void blockUser(int userId) {
		String sql = "UPDATE Users SET Blocked = 1 WHERE Id = ?";
        jdbcTemplate.update(sql, userId);
	}

	@Override
	public void unblockUser(int userId) {
		String sql = "UPDATE Users SET Blocked = 0 WHERE Id = ?";
        jdbcTemplate.update(sql, userId);
	}

	@Override
	public void deactivateUser(int userId) {
		 String sql = "UPDATE Users SET Active = 0 WHERE Id = ?";
	     jdbcTemplate.update(sql, userId);
	}

	@Override
	public void activateUser(int userId) {
		String sql = "UPDATE Users SET Active = 1 WHERE Id = ?";
        jdbcTemplate.update(sql, userId);
	}

	@Override
	public List<BookCategory> getAllCategories() {
        String sql = "SELECT * FROM BookCategories";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(BookCategory.class));
	}

	@Override
	public void insertNewBook(Book book) {
		String getCategorySql = "SELECT Id FROM BookCategories WHERE Category = ? AND SubCategory = ?";
        Integer categoryId = jdbcTemplate.queryForObject(
                getCategorySql,
                Integer.class,
                book.getCategory().getCategory(),
                book.getCategory().getSubCategory()
        );

        String insertBookSql = "INSERT INTO Books (Title, Author, Price, Ordered, CategoryId) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(
                insertBookSql,
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                false,
                categoryId
        );

	}

	@Override
	public boolean deleteBook(int bookId) {
		String sql = "DELETE FROM Books WHERE Id = ?";
        int deletedRows = jdbcTemplate.update(sql, bookId);
        return deletedRows == 1;
	}

	@Override
	public void createCategory(BookCategory bookCategory) {
        String sql = "INSERT INTO BookCategories (Category, SubCategory) VALUES (?, ?)";
        jdbcTemplate.update(sql, bookCategory.getCategory(), bookCategory.getSubCategory());
	}

//	@Override
//	public int fetchfine(int userId)
//	{
//		String sql = "Select amount from fine WHERE UserId = ?";
//        int amount = jdbcTemplate.query(sql, userId);
//        return amount;
//	}
}
