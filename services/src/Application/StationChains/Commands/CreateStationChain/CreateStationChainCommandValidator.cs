﻿using FluentValidation;

namespace Application.StationChains.Commands.CreateStationChain;

public sealed class CreateStationChainCommandValidator : AbstractValidator<CreateStationChainCommand>
{
    public CreateStationChainCommandValidator()
    {
        RuleFor(e => e.Name)
            .NotEmpty()
            .MinimumLength(3)
            .MaximumLength(128);
    }
}