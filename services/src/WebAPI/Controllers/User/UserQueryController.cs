using Application.Models;
using Application.Models.Pagination;
using Application.Users.Queries.GetAllUsers;
using Application.Users.Queries.GetLoggedUser;
using Application.Users.Queries.GetUser;
using Domain.Common.Pagination.Response;
using MediatR;
using Microsoft.AspNetCore.Mvc;
using WebAPI.Common.Authorization;

namespace WebAPI.Controllers.User;

[ApiController]
[AuthorizeUser]
[Route("api/v1/users")]
public class UserQueryController : ControllerBase
{
    private readonly IMediator _mediator;

    public UserQueryController(IMediator mediator)
    {
        _mediator = mediator;
    }
    
    [HttpGet("{username}")]
    public async Task<ActionResult<UserDto>> GetInfoAboutUser([FromRoute] string username)
    {
        var result = await _mediator.Send(new GetUserQuery(username));
        return Ok(result);
    }
    
    [AuthorizeAdmin]
    [HttpGet]
    public async Task<ActionResult<Page<UserDetailsDto>>> GetAllAsync([FromQuery] PageRequestDto pageRequestDto)
    {
        var result = await _mediator.Send(new GetAllUsersQuery(pageRequestDto));
        return Ok(result);
    }
    
    [HttpGet("logged-user")]
    public async Task<ActionResult<UserDetailsDto>> GetInfoAboutLoggedUser()
    {
        var result = await _mediator.Send(new GetLoggedUserQuery());
        return Ok(result);
    }
}


