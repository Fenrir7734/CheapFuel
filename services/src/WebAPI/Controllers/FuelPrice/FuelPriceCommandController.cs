using Application.FuelPrices.Commands.ExtractFuelPrices;
using Application.FuelPrices.Commands.UpdateFuelPriceByOwner;
using Application.Models.FuelPriceDtos;
using MediatR;
using Microsoft.AspNetCore.Mvc;
using WebAPI.Common.Authorization;
using WebAPI.Models;

namespace WebAPI.Controllers.FuelPrice;

[ApiController]
[Route("api/v1/fuel-prices")]
public sealed class FuelPriceCommandController : ControllerBase
{
    private readonly IMediator _mediator;
    private readonly IWebHostEnvironment _environment;

    public FuelPriceCommandController(IMediator mediator, IWebHostEnvironment environment)
    {
        _mediator = mediator;
        _environment = environment;
    }

    [AuthorizeOwner]
    [HttpPost]
    public async Task<ActionResult<IEnumerable<FuelPriceDto>>> UpdateFuelPrices([FromBody] NewFuelPricesAtStationDto dto)
    {
        var result = await _mediator.Send(new UpdateFuelPriceByOwnerCommand(dto));
        return Ok(result); // Should be updated after creating endpoint to get fuel prices
    }

    [HttpPost("extract")]
    public async Task<ActionResult> Post([FromForm] FuelPriceUpload fuelPriceUpload)
    {
        // await using var fileStream = System.IO.File.Create(_environment.WebRootPath + "\\Upload\\" + objFile.File.FileName);
        // await objFile.File.CopyToAsync(fileStream);
        // fileStream.Flush();
        // return await Task.FromResult(Ok());
        var command = new ExtractFuelPricesCommand(
            FuelStationId: fuelPriceUpload.FuelStationId,
            UserLongitude: fuelPriceUpload.UserLongitude,
            UserLatitude: fuelPriceUpload.UserLatitude,
            Image: fuelPriceUpload.ImageAsBytes(),
            ContentType: fuelPriceUpload.Image?.ContentType);
        
        var result = await _mediator.Send(command);
        return Ok(result);
    }
}