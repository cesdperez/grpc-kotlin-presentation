package barservice

import barservice.generated.Drink
import barservice.generated.DrinkRequest
import barservice.generated.DrinkType
import barservice.generated.DrinksRequest
import barservice.generated.PaymentRequest
import io.grpc.Status
import io.grpc.StatusException

fun DrinkRequest.assertValid() {
    if (!this.hasDrink()) {
        throw StatusException(Status.FAILED_PRECONDITION.withDescription("drink is required"))
    }
    if (this.drink.type == DrinkType.NONE) {
        throw StatusException(Status.FAILED_PRECONDITION.withDescription("drink.type is required"))
    }
}

fun DrinksRequest.assertValid() {
    if (this.drinksList.isEmpty()) {
        throw StatusException(Status.FAILED_PRECONDITION.withDescription("drinks is required"))
    }
    this.drinksList.forEach {
        if (it.type == DrinkType.NONE) {
            throw StatusException(Status.FAILED_PRECONDITION.withDescription("drink.type is required"))
        }
    }
}

fun PaymentRequest.assertValid() {
    if (this.billId.isEmpty()) {
        throw StatusException(Status.FAILED_PRECONDITION.withDescription("billId is required"))
    }
}

fun Drink.getPrice(): Long = when (this.type) {
    DrinkType.BEER -> 100
    DrinkType.VODKA -> 150
    DrinkType.RUM -> 50
    DrinkType.WHISKY -> 300
    DrinkType.GIN -> 200
    DrinkType.WINE -> 250
    else -> throw Exception("Unsupported drink type")
}

