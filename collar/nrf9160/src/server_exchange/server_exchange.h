#ifndef SERVER_EXCHANGE_H
#define SERVER_EXCHANGE_H

/* @brief Init server exchange
 * @return 0 on succes
 */
int server_exchange_init();
int server_connect();

extern bool isInitialized;
void start_polling();
void stop_polling();


#endif