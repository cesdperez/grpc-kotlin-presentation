package barclient

import barservice.generated.*
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.Closeable
import java.util.concurrent.TimeUnit

suspend fun main() {
    val port = 8080
    val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()
    val client = BarClient(channel)

    val bills = mutableListOf<Bill>()
    bills.add(
        client.orderDrink(
            name = "Johnnie Walker Blue Label",
            type = DrinkType.WHISKY,
        )
    )
    Thread.sleep(2000)

    bills.add(
        client.orderDrink(
            name = "Absolut",
            type = DrinkType.VODKA,
        )
    )
    Thread.sleep(2000)

    bills.add(
        client.orderDrink(
            name = "Kingfisher Ultra",
            type = DrinkType.BEER,
        )
    )
    Thread.sleep(2000)

    bills.forEach {
        client.payBill(it, 1000)
        Thread.sleep(2000)
    }

    val dummyBill = Bill.newBuilder()
        .setId("123")
        .setAmount(100)
        .build()
    client.payBill(dummyBill, 300)
}

class BarClient(private val channel: ManagedChannel) : Closeable {

    private val barStub: BarGrpcKt.BarCoroutineStub = BarGrpcKt.BarCoroutineStub(channel)

    suspend fun orderDrink(name: String, type: DrinkType): Bill {
        val drink = Drink.newBuilder()
            .setName(name)
            .setType(type)
            .build()

        val request = DrinkRequest.newBuilder()
            .setDrink(drink)
            .build()

        println("Ordering drink:\n$request")
        val response = barStub.orderDrink(request)
        println("Received from Bar:\n$response")

        return response.bill
    }

    suspend fun payBill(bill: Bill, paymentAmount: Long): Long {
        val request = PaymentRequest.newBuilder()
            .setBillId(bill.id)
            .setPaymentAmount(paymentAmount)
            .build()

        println("Paying bill:\n$request")
        val response = barStub.payBill(request)
        println("Received from Bar:\n$response")

        return response.change
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
