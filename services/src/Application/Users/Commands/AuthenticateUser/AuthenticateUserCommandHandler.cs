using Application.Common.Authentication;
using Application.Common.Exceptions;
using Domain.Interfaces;
using Domain.Interfaces.Repositories;
using MediatR;
using Microsoft.Extensions.Logging;

namespace Application.Users.Commands.AuthenticateUser;

public sealed class AuthenticateUserCommandHandler : IRequestHandler<AuthenticateUserCommand, string>
{
    private readonly IUserRepository _userRepository;
    private readonly IUserPasswordHasher _passwordHasher;
    private readonly ITokenService _tokenService;
    private readonly ILogger<AuthenticateUserCommandHandler> _logger;
    public AuthenticateUserCommandHandler(IUnitOfWork unitOfWork, IUserPasswordHasher passwordHasher, ITokenService tokenService, ILogger<AuthenticateUserCommandHandler> logger)
    {
        _userRepository = unitOfWork.Users;
        _passwordHasher = passwordHasher;
        _tokenService = tokenService;
        _logger = logger;
    }
    
    public async Task<string> Handle(AuthenticateUserCommand request, CancellationToken cancellationToken)
    {
        
        var user = await _userRepository.GetByUsernameAsync(request.Username);

        if (user is null)
        {
            throw new UnauthorizedException("Invalid email or password");
        }

        var isPasswordCorrect = _passwordHasher.IsPasswordCorrect(user.Password, request.Password, user);
       
        if (!isPasswordCorrect)
        {
            throw new UnauthorizedException("Invalid email or password");
        }
        _logger.LogInformation("User Login");
        return _tokenService.GenerateToken(user);
    }
}