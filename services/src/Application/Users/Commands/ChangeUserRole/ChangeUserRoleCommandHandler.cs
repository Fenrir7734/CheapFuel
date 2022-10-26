using Application.Common.Exceptions;
using Domain.Interfaces;
using Domain.Interfaces.Repositories;
using MediatR;
using Microsoft.Extensions.Logging;

namespace Application.Users.Commands.ChangeUserRole;

public sealed class ChangeUserRoleCommandHandler : IRequestHandler<ChangeUserRoleCommand>
{
    private readonly IUnitOfWork _unitOfWork;
    private readonly IUserRepository _userRepository;
    private readonly  ILogger<ChangeUserRoleCommandHandler> _logger;

    public ChangeUserRoleCommandHandler(IUnitOfWork unitOfWork, IUserRepository userRepository, ILogger<ChangeUserRoleCommandHandler> logger)
    {
        _unitOfWork = unitOfWork;
        _userRepository = userRepository;
        _logger = logger;
    }

    public async Task<Unit> Handle(ChangeUserRoleCommand request, CancellationToken cancellationToken)
    {
        var user = await _userRepository.GetByUsernameAsync(request.Username)
                   ?? throw new NotFoundException($"User not found for username = {request.Username}");

        user.Role = request.Role;
        await _unitOfWork.SaveAsync();
        _logger.LogInformation("Role changed");
        return Unit.Value;
    }
}