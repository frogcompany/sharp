<?php

use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider and all of them will
| be assigned to the "web" middleware group. Make something great!
|
*/

Route::get('/', function () {
    return view('welcome');
});

use App\Http\Controllers\ChatGptController;
use App\Http\Controllers\DetailController;

Route::get('/chat', [ChatGptController::class, 'index'])->name('chat_gpt-index');
Route::post('/chat', [ChatGptController::class, 'chat'])->name('chat_gpt-chat');
Route::get('/detail/{id}', [DetailController::class, 'index'])->name('detail-index');


Auth::routes();

Route::get('/home', [App\Http\Controllers\HomeController::class, 'index'])->name('home');
