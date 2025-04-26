import { useState } from 'react';
import './App.css';
import axios from 'axios';

function App() {
  const [row, setRow] = useState(0);
  const [col, setColumns] = useState(0);
  const [mines, setMines] = useState(0);
  const [board, setBoard] = useState([]);
  const [clicked, setClicked] = useState(false);
  const [mode, setMode] = useState('');

  const handleSubmit = async () => {
    try {
      const response = await axios.post('http://localhost:8080/api/genera', {
        row: row,
        col: col,
        mines: mines
      });
      setBoard(response.data); 
      setClicked(false); // resettiamo la board come nuova
    } catch (error) {
      console.error(error);
    }
  };

  const handleCellClick = async (rowIndex, colIndex) => {
    try {
      if (!clicked) {
        const response = await axios.post('http://localhost:8080/api/clic', {
          row: rowIndex,
          col: colIndex
        });
        setClicked(true);
        setBoard(response.data);
      } else {
        const response = await axios.post('http://localhost:8080/api/reveal', {
          row: rowIndex,
          col: colIndex
        });
        setBoard(response.data);
      }
    } catch (error) {
      console.error('Errore nel click:', error);
    }
  };

  const handleModeSelect = (selectedMode) => {
    setMode(selectedMode);
    if (selectedMode === 'facile') {
      setColumns(8);
      setRow(8);
      setMines(10);
    } else if (selectedMode === 'medio') {
      setColumns(16);
      setRow(16);
      setMines(40);
    } else if (selectedMode === 'difficile') {
      setColumns(30);
      setRow(16);
      setMines(99);
    } else if (selectedMode === 'personalizzato') {
      setColumns(0);
      setRow(0);
      setMines(0);
    }
  };

  return (
    <>
      <div style={{ position: 'relative', minHeight: '100vh' }}>
        {/* HEADER pulsanti modalità */}
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            display: 'flex',
            justifyContent: 'center',
            gap: '20px',
            padding: '20px',
            zIndex: 10,
            flexWrap: 'nowrap',
          }}
        >
          {['Facile', 'Medio', 'Difficile', 'Personalizzato'].map((label) => (
            <button
              key={label}
              onClick={() => handleModeSelect(label.toLowerCase())}
              style={{
                padding: '10px 20px',
                backgroundColor: mode === label.toLowerCase() ? '#aaa' : '#fff',
                border: '1px solid #000',
                borderRadius: '5px',
                cursor: 'pointer',
                color: '#000',
              }}
            >
              {label}
            </button>
          ))}
        </div>

        {/* INPUTS per personalizzato */}
        {mode === 'personalizzato' && (
          <div
            style={{
              position: 'absolute',
              top: '80px',
              left: 0,
              width: '100%',
              display: 'flex',
              justifyContent: 'center',
              gap: '20px',
              padding: '10px',
              zIndex: 9,
            }}
          >
            <label style={{ color: 'white' }}>
              Colonne:
              <input
                type="number"
                value={col}
                onChange={(e) => setColumns(Number(e.target.value))}
                style={{ padding: '5px', width: '80px', marginLeft: '5px' }}
              />
            </label>

            <label style={{ color: 'white' }}>
              Righe:
              <input
                type="number"
                value={row}
                onChange={(e) => setRow(Number(e.target.value))}
                style={{ padding: '5px', width: '80px', marginLeft: '5px' }}
              />
            </label>

            <label style={{ color: 'white' }}>
              Mine:
              <input
                type="number"
                value={mines}
                onChange={(e) => setMines(Number(e.target.value))}
                style={{ padding: '5px', width: '80px', marginLeft: '5px' }}
              />
            </label>
          </div>
        )}

        {/* CONTENUTO */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            paddingTop: mode === 'personalizzato' ? '160px' : '120px',
          }}
        >
          <button
            onClick={handleSubmit}
            style={{
              padding: '12px 42px',
              fontSize: '16px',
              border: 'none',
              borderRadius: '5px',
              backgroundColor: '#ffffff',
              color: '#000000',
              cursor: 'pointer',
              marginBottom: '30px',
            }}
          >
            Genera
          </button>

          {/* GRIGLIA */}
          {board.length > 0 && (
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: '30px' }}>
              <table style={{ borderCollapse: 'collapse' }}>
                <tbody>
                  {board.map((rowArray, rowIndex) => (
                    <tr key={rowIndex}>
                      {rowArray.map((cell, colIndex) => (
                        <td
                          key={colIndex}
                          style={{
                            border: '1px solid black',
                            width: '30px',
                            height: '30px',
                          }}
                        >
                          {cell === null ? (
                            <button
                              onClick={() => handleCellClick(rowIndex, colIndex)}
                              style={{
                                width: '100%',
                                height: '100%',
                                backgroundColor: '#ccc',
                                border: 'none',
                                cursor: 'pointer',
                              }}
                            />
                          ) : (
                            <div
                              style={{
                                textAlign: 'center',
                                lineHeight: '30px',
                                backgroundColor: '#eee',
                                color: getColor(cell),
                              }}
                            >
                              {cell === -1 ? '💣' : cell === 0 ? '' : cell}
                            </div>
                          )}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

function getColor(number) {
  switch (number) {
    case 1: return 'blue';
    case 2: return 'green';
    case 3: return 'red';
    case 4: return 'darkblue';
    case 5: return 'darkred';
    case 6: return 'turquoise';
    case 7: return 'black';
    case 8: return 'gray';
    default: return 'black';
  }
}

export default App;
