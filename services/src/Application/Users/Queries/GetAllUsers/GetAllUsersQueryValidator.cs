using Application.Models.Pagination;
using FluentValidation;

namespace Application.Users.Queries.GetAllUsers;

public sealed class GetAllUserQueryValidator : AbstractValidator<GetAllUsersQuery>
{
    public GetAllUserQueryValidator()
    {
        RuleFor(g => g.PageRequestDto)
            .SetValidator(new PageRequestDtoValidator());
    }
}