using Application.Common;
using Application.Common.Exceptions;
using Application.Common.Interfaces;
using Domain.Interfaces;
using Domain.Interfaces.Repositories;
using MediatR;

namespace Application.FuelPrices.Commands.ExtractFuelPrices;

public class ExtractFuelPricesCommandHandler : IRequestHandler<ExtractFuelPricesCommand>
{
    private const double AllowedDistanceThreshold = 200.0;
    
    private readonly IFuelStationRepository _fuelStationRepository;
    private readonly IOcrService _ocrService;

    public ExtractFuelPricesCommandHandler(IUnitOfWork unitOfWork, IOcrService ocrService)
    {
        _fuelStationRepository = unitOfWork.FuelStations;
        _ocrService = ocrService;
    }
    
    public async Task<Unit> Handle(ExtractFuelPricesCommand request, CancellationToken cancellationToken)
    {
        var fuelStation = await _fuelStationRepository.GetAsync(request.FuelStationId!.Value)
                          ?? throw new NotFoundException($"Fuel station not found for id = {request.FuelStationId}");

        var distance = Utlis.GetDistance(
            (double)fuelStation.GeographicalCoordinates!.Longitude,
            (double)fuelStation.GeographicalCoordinates!.Latitude, 
            request.UserLongitude!.Value, 
            request.UserLatitude!.Value);

        if (distance > AllowedDistanceThreshold)
        {
            throw new BadRequestException("User location is too far from fuel station location");
        }
        
        await _ocrService.Extract(request.Image);
        return await Unit.Task;
    }
}