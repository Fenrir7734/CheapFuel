using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Application.Common.Exceptions;
using Application.Models;
using Application.Models.Pagination;
using Application.Users.Queries.GetAllUsers;
using AutoMapper;
using Domain.Common.Pagination.Request;
using Domain.Common.Pagination.Response;
using Domain.Entities;
using Domain.Enums;
using Domain.Interfaces;
using Domain.Interfaces.Repositories;
using FluentAssertions;
using Moq;
using Xunit;

namespace Application.UnitTests.Users.Queries.GetAllUsers;

public class GetAllUsersQueryHandlerTest
{
    private readonly Mock<IUserRepository> _userRepository;
    private readonly Mock<IMapper> _mapper;
    private readonly GetAllUserQueryHandler _handler;

    public GetAllUsersQueryHandlerTest()
    {
        _userRepository = new Mock<IUserRepository>();
        var unitOfWork = new Mock<IUnitOfWork>();
        unitOfWork
            .Setup(u => u.Users)
            .Returns(_userRepository.Object);
        _mapper = new Mock<IMapper>();

        _handler = new GetAllUserQueryHandler(unitOfWork.Object, _mapper.Object);
    }

    [Fact]
    public async Task Returns_page_of_users()
    {
        // Arrange
        var pageRequestDto = new PageRequestDto { PageSize = 1, PageNumber = 10, Sort = null };
        var pageRequest = new PageRequest<User>
        {
            PageNumber = (int)pageRequestDto.PageNumber,
            PageSize = (int)pageRequestDto.PageSize,
            Sort = null
        };
        var query = new GetAllUsersQuery(pageRequestDto);

        var data = CreateData();
        var dataDtos = CreateDto(data);
        var users = CreatePage(pageRequest, data);

        _userRepository
            .Setup(x => x.GetAllAsync(It.IsAny<PageRequest<User>>()))
            .ReturnsAsync(users);

        _mapper
            .Setup(x => x.Map<IEnumerable<UserDetailsDto>>(data))
            .Returns(dataDtos);
        
        // Act
        var result = await _handler.Handle(query, CancellationToken.None);
        
        // Assert
        result.Should().NotBeNull();
        result.Data.Should().NotBeNull();
        result.Data.Should().HaveCount(2);
    }

    [Fact]
    public async Task Throw_exception_for_invalid_sort_column()
    {
        // Arrange
        const string column = "Invalid column";

        var pageRequestDto = new PageRequestDto()
        {
            PageSize = 1,
            PageNumber = 10,
            Sort = new SortDto { SortBy = column, SortDirection = SortDirection.Asc}
        };
        var query = new GetAllUsersQuery(pageRequestDto);
        
        // Act
        Func<Task<Page<UserDetailsDto>>> act = _handler.Awaiting(x => x.Handle(query, CancellationToken.None));
        
        // Assert
        await act
            .Should()
            .ThrowAsync<BadRequestException>();
        
        _userRepository.Verify(x => x.GetAllAsync(It.IsAny<PageRequest<User>>()), Times.Never);
        _mapper.Verify(x => x.Map<IEnumerable<UserDetailsDto>>(It.IsAny<IEnumerable<User>>()), Times.Never);
    }

    private List<User> CreateData() => new()
    {
        new()
        {
            Username = "Grzesio",
            Email = "grzesio@gmail.com",
            EmailConfirmed = true,
            Role = Role.User,
            CreatedAt = new DateTime(2022, 11, 1)
        },
        new()
        {
            Username = "Kazio",
            Email = "kazio@gmail.com",
            EmailConfirmed = true,
            Role = Role.Admin,
            CreatedAt = new DateTime(2022, 10, 1)
        },
    };

    private List<UserDetailsDto> CreateDto(List<User> data) => new()
    {
        new()
        {
            Username = data[0].Username,
            Email = data[0].Email,
            EmailConfirmed = data[0].EmailConfirmed,
            MultiFactorAuthEnabled = data[0].MultiFactorAuthEnabled,
            Role = data[0].Role,
            Status = data[0].Status,
            CreatedAt = data[0].CreatedAt
        },
        new()
        {
            Username = data[1].Username,
            Email = data[1].Email,
            EmailConfirmed = data[1].EmailConfirmed,
            MultiFactorAuthEnabled = data[1].MultiFactorAuthEnabled,
            Role = data[1].Role,
            Status = data[1].Status,
            CreatedAt = data[1].CreatedAt
        }
    };
    
    private Page<E> CreatePage<E>(PageRequest<User> pageRequest, IEnumerable<E> data) => new()
    {
        PageNumber = pageRequest.PageNumber,
        PageSize = pageRequest.PageSize,
        NextPage = null,
        PreviousPage = null,
        FirstPage = 1,
        LastPage = 1,
        TotalPages = 1,
        TotalElements = 2,
        Sort = null,
        Data = data
    };
}
