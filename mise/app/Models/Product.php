<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Concerns\HasUlids;

class Product extends Model
{
    use HasFactory,HasUlids;

    public $table = 'products';
    
    protected $primaryKey = 'id';
    protected $fillable = [
        'name',
    ];


}

