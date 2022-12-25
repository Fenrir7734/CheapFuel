using MediatR;

namespace Application.FuelPrices.Commands.ExtractFuelPrices;

public sealed record ExtractFuelPricesCommand(
    long? FuelStationId, 
    double? UserLongitude, 
    double? UserLatitude,
    byte[] Image,
    string? ContentType) : IRequest<Unit>;