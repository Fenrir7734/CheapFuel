using Application.Common.Authentication;
using Application.Common.Exceptions;
using Application.Models;
using Application.Users.Commands.ChangeUserRole;
using AutoMapper;
using Domain.Entities;
using Domain.Enums;
using Domain.Interfaces;
using Domain.Interfaces.Repositories;
using MediatR;
using Microsoft.Extensions.Logging;

namespace Application.Users.Commands.RegisterUser;

public class RegisterUserCommandHandler : IRequestHandler<RegisterUserCommand, UserDto>
{
    private readonly IUnitOfWork _unitOfWork;
    private readonly IUserRepository _userRepository;
    private readonly IMapper _mapper;
    private readonly IUserPasswordHasher _passwordHasher;
    private readonly  ILogger<ChangeUserRoleCommandHandler> _logger;
    public RegisterUserCommandHandler(IUnitOfWork unitOfWork, IMapper mapper, IUserPasswordHasher passwordHasher, ILogger<ChangeUserRoleCommandHandler> logger)
    {
        _unitOfWork = unitOfWork;
        _userRepository = unitOfWork.Users;
        _mapper = mapper;
        _passwordHasher = passwordHasher;
        _logger = logger;
    }
    
    public async Task<UserDto> Handle(RegisterUserCommand request, CancellationToken cancellationToken)
    {
        if (await _userRepository.ExistsByUsername(request.Username))
        {
            throw new DuplicateCredentialsException("Username is already taken");
        }

        if (await _userRepository.ExistsByEmail(request.Email))
        {
            throw new DuplicateCredentialsException("Email is already taken");
        }

        var newUser = new User
        {
            Username = request.Username,
            Email = request.Email,
            EmailConfirmed = true,
            MultiFactorAuthEnabled = false,
            Status = AccountStatus.Active,
            Role = Role.User
        };

        var hashedPassword = _passwordHasher.HashPassword(request.Password, newUser);
        newUser.Password = hashedPassword;
        
        _userRepository.Add(newUser);
        await _unitOfWork.SaveAsync();
        _logger.LogInformation("User registered"); 
        return _mapper.Map<UserDto>(newUser);
        
    }
}