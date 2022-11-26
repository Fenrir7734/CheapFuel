using System.Net;
using System.Net.Mime;
using System.Threading.Tasks;
using Application.Models;
using Domain.Common.Pagination.Request;
using Domain.Common.Pagination.Response;
using Domain.Entities;
using FluentAssertions;
using WebAPI.IntegrationTests.PredefinedData;
using WebAPI.IntegrationTests.TestConfiguration;
using Xunit;
using Xunit.Abstractions;

namespace WebAPI.IntegrationTests.Users;

public class UserQueryControllerTest : IntegrationTest
{
    public UserQueryControllerTest(
        TestingWebApiFactory<Program> factory,
        ITestOutputHelper outputHelper)
        : base(
            factory,
            outputHelper,
            new IPredefinedData[] { new AccountsData(), new UserQueryControllerData() }) { }

    [Fact]
    public async Task Returns_page_of_users()
    {
        // Arrange
        await this.AuthorizeAdmin();
        
        const int pageNumber = 1;
        const int pageSize = 10;
        const string sortBy = nameof(User.Username);
        const SortDirection direction = SortDirection.Asc;
        
        // Act
        var response = await HttpClient.GetAsync(
            "api/v1/users?" +
            $"PageNumber={pageNumber}&" +
            $"PageSize={pageSize}&" +
            $"Sort.SortBy={sortBy}&" +
            $"Sort.SortDirection={direction}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
        response.Content.Headers.ContentType?.MediaType.Should().Be(MediaTypeNames.Application.Json);

        var page = await this.Deserialize<Page<UserDetailsDto>>(response.Content);
        page!.PageNumber.Should().Be(pageNumber);
        page.PageSize.Should().Be(pageSize);
        page.TotalElements.Should().Be(UserQueryControllerData.InitialUsersCount + AccountsData.InitialUserCount);
        page.Data.Should().NotBeNull().And.HaveCount(UserQueryControllerData.InitialUsersCount + AccountsData.InitialUserCount);
    }
    
    [Fact]
    public async Task Fails_to_return_page_of_users_if_requested_by_not_admin()
    {
        // Arrange
        await this.AuthorizeUser();
        
        const int pageNumber = 1;
        const int pageSize = 10;
        const string sortBy = nameof(User.Username);
        const SortDirection direction = SortDirection.Asc;
        
        // Act
        var response = await HttpClient.GetAsync(
            "api/v1/users?" +
            $"PageNumber={pageNumber}&" +
            $"PageSize={pageSize}&" +
            $"Sort.SortBy={sortBy}&" +
            $"Sort.SortDirection={direction}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Forbidden);
        response.Content.Headers.ContentType?.MediaType.Should().Be(MediaTypeNames.Application.Json);
    }

    [Fact]
    public async Task Returns_info_about_logged_user()
    {
        // Arrange
        await this.AuthorizeGenericUser(UserQueryControllerData.User1Username, AccountsData.DefaultPassword);

        // Act
        var response = await HttpClient.GetAsync("api/v1/users/logged-user");
        
        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
        response.Content.Headers.ContentType?.MediaType.Should().Be(MediaTypeNames.Application.Json);

        var user = await this.Deserialize<UserDetailsDto>(response.Content);
        user!.Username.Should().Be(UserQueryControllerData.User1Username);
        user.Email.Should().NotBeNull();
        user.EmailConfirmed.Should().NotBeNull();
        user.Status.Should().NotBe(null);
        user.Role.Should().NotBe(null);
        user.CreatedAt.Should().NotBe(null);
    }

    [Fact]
    public async Task Fails_to_return_info_about_logged_user_if_user_is_not_logged_in()
    {
        // Act
        var response = await HttpClient.GetAsync("api/v1/users/logged-user");
        
        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized);
        response.Content.Headers.ContentType?.MediaType.Should().Be(MediaTypeNames.Application.Json);
    }

    [Fact]
    public async Task Return_info_about_user_with_given_username()
    {
        // Arrange
        await this.AuthorizeUser();
        
        const string username = UserQueryControllerData.User1Username;

        // Act 
        var response = await HttpClient.GetAsync($"api/v1/users/{username}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
        response.Content.Headers.ContentType?.MediaType.Should().Be(MediaTypeNames.Application.Json);

        var user = await this.Deserialize<UserDto>(response.Content);
        user!.Username.Should().Be(username);
        user.Role.Should().NotBe(null);
        user.Status.Should().NotBe(null);
        user.CreatedAt.Should().NotBe(null);
    }
    
    [Fact]
    public async Task Fails_to_return_info_about_user_with_given_username_if_requested_by_not_logged_in_user()
    {
        // Arrange
        const string username = UserQueryControllerData.User1Username;

        // Act 
        var response = await HttpClient.GetAsync($"api/v1/users/{username}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized);
        response.Content.Headers.ContentType?.MediaType.Should().Be(MediaTypeNames.Application.Json);
    }

    [Fact]
    public async Task Fails_to_return_info_about_user_with_given_username_if_user_not_exists()
    {
        // Arrange
        await this.AuthorizeUser();

        const string username = AccountsData.InvalidUsername;

        // Act 
        var response = await HttpClient.GetAsync($"api/v1/users/{username}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
        response.Content.Headers.ContentType?.MediaType.Should().Be(MediaTypeNames.Application.Json);
    }
}