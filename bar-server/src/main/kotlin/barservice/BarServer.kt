package barservice

import barservice.generated.*
import com.google.protobuf.timestamp
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.protobuf.services.ProtoReflectionService
import java.time.Instant
import java.util.*

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val server = BarServer(port)
    server.start()
    server.blockUntilShutdown()
}

class BarServer(private val port: Int) {

    private val server: Server = ServerBuilder
        .forPort(port)
        .addService(BarService())
        .addService(ProtoReflectionService.newInstance())
        .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@BarServer.stop()
                println("*** server shut down")
            }
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    private class BarService : BarGrpcKt.BarCoroutineImplBase() {

        val generatedBills = mutableMapOf<String, Bill>()
        val billStatus = mutableMapOf<String, PaymentStatus>()

        override suspend fun orderDrink(request: DrinkRequest): DrinkResponse {
            request.assertValid()

            println("Requested order:\n$request")
            val bill = generateBill(request.drink)
            generatedBills[bill.id] = bill
            billStatus[bill.id] = PaymentStatus.PENDING
            println("Generated bill $bill with status ${PaymentStatus.PENDING}")

            return drinkResponse {
                this.bill = bill
            }
        }

        override suspend fun orderMultipleDrinks(request: DrinksRequest): DrinkResponse {
            request.assertValid()

            println("Requested order:\n$request")
            val bill = generateBill(*request.drinksList.toTypedArray())
            generatedBills[bill.id] = bill
            billStatus[bill.id] = PaymentStatus.PENDING
            println("Generated bill $bill with status ${PaymentStatus.PENDING}")

            return drinkResponse {
                this.bill = bill
            }
        }

        override suspend fun payBill(request: PaymentRequest): PaymentResponse {
            request.assertValid()

            return when (billStatus[request.billId]) {
                PaymentStatus.PAID -> {
                    paymentResponse {
                        status = PaymentStatus.FAILED
                        change = request.paymentAmount
                        reason = "Bill ${request.billId} is already paid"
                    }
                }
                PaymentStatus.PENDING, PaymentStatus.FAILED -> {
                    val bill = generatedBills.getValue(request.billId)
                    if (bill.amount > request.paymentAmount) {
                        billStatus[request.billId] = PaymentStatus.FAILED
                        throw StatusException(Status.FAILED_PRECONDITION.withDescription("paymentAmount has to be bigger than the bill amount (which is ${bill.amount})"))
                    }

                    billStatus[request.billId] = PaymentStatus.PAID
                    paymentResponse {
                        status = PaymentStatus.PAID
                        change = request.paymentAmount - bill.amount
                    }
                }
                else -> {
                    paymentResponse {
                        status = PaymentStatus.FAILED
                        change = request.paymentAmount
                        reason = "There is no bill with ID ${request.billId}"
                    }
                }
            }
        }

        override suspend fun getBillsStatus(request: BillsStatusRequest): BillsStatusResponse {
            val filtered = billStatus
                .filter { !request.hasId() || it.key == request.id }
                .filter { !request.hasStatus() || it.value == request.status }

            return billsStatusResponse {
                bills.putAll(filtered)
            }
        }

        fun generateBill(vararg drinks: Drink): Bill {
            val orderId = UUID.randomUUID().toString()
            val time = Instant.now()
            val bill = bill {
                id = orderId
                this.amount = drinks.sumOf { it.getPrice() }
                timestamp = timestamp {
                    seconds = time.epochSecond
                    nanos = time.nano
                }
            }

            generatedBills[orderId] = bill
            billStatus[orderId] = PaymentStatus.PENDING
            return bill
        }
    }
}
